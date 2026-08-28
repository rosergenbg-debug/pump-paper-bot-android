#!/usr/bin/env node

/*
 * Causal research harness for PUMP/BTC/SOL one-minute Binance candles.
 * It is deliberately detached from Android trading authority: the purpose is to measure
 * whether broad-market turns add out-of-sample information to a PUMP bottom candidate.
 */

const fs = require("fs");
const path = require("path");

const API = "https://data-api.binance.vision/api/v3/klines";
const MINUTE = 60_000;
const DAYS = Number(process.argv[2] || 7);
const OUT = process.argv[3] || path.join("/tmp", `pump-cross-market-${DAYS}d.json`);

function pct(a, b) { return a > 0 ? (b / a - 1) * 100 : 0; }
function mean(xs) { return xs.length ? xs.reduce((a, b) => a + b, 0) / xs.length : 0; }
function median(xs) {
  if (!xs.length) return 0;
  const s = [...xs].sort((a, b) => a - b);
  const m = Math.floor(s.length / 2);
  return s.length % 2 ? s[m] : (s[m - 1] + s[m]) / 2;
}
function corr(a, b) {
  const ma = mean(a), mb = mean(b);
  let n = 0, da = 0, db = 0;
  for (let i = 0; i < a.length; i++) {
    const x = a[i] - ma, y = b[i] - mb;
    n += x * y; da += x * x; db += y * y;
  }
  return da && db ? n / Math.sqrt(da * db) : 0;
}

async function fetchWeek(symbol, start, end) {
  const rows = [];
  for (let cursor = start; cursor < end;) {
    const url = `${API}?symbol=${symbol}&interval=1m&startTime=${cursor}&endTime=${end - 1}&limit=1000`;
    const response = await fetch(url);
    if (!response.ok) throw new Error(`${symbol}: HTTP ${response.status}`);
    const page = await response.json();
    if (!page.length) break;
    rows.push(...page);
    cursor = Number(page[page.length - 1][0]) + MINUTE;
  }
  return rows.map(r => ({
    t: Number(r[0]), open: Number(r[1]), high: Number(r[2]), low: Number(r[3]),
    close: Number(r[4]), quote: Number(r[7]), trades: Number(r[8]),
    takerBuyQuote: Number(r[10])
  }));
}

function align(series) {
  const maps = Object.fromEntries(Object.entries(series).map(([k, rows]) => [k, new Map(rows.map(r => [r.t, r]))]));
  const times = [...maps.PUMP.keys()].filter(t => maps.BTC.has(t) && maps.SOL.has(t)).sort((a, b) => a - b);
  return times.map(t => ({ t, PUMP: maps.PUMP.get(t), BTC: maps.BTC.get(t), SOL: maps.SOL.get(t) }));
}

function feature(rows, i) {
  const c = key => rows[i][key].close;
  const r = (key, n) => pct(rows[i - n][key].close, c(key));
  const previous = (key, from, to) => pct(rows[i - from][key].close, rows[i - to][key].close);
  const low60 = Math.min(...rows.slice(i - 59, i + 1).map(x => x.PUMP.low));
  const high60 = Math.max(...rows.slice(i - 59, i + 1).map(x => x.PUMP.high));
  const rangePosition = high60 > low60 ? (c("PUMP") - low60) / (high60 - low60) : 0.5;
  const btc5 = r("BTC", 5), sol5 = r("SOL", 5);
  const btcPrev5 = previous("BTC", 10, 5), solPrev5 = previous("SOL", 10, 5);
  return {
    pump30: r("PUMP", 30), pump5: r("PUMP", 5), pump3: r("PUMP", 3),
    rangePosition, btc5, sol5, btc15: r("BTC", 15), sol15: r("SOL", 15),
    btcAcceleration: btc5 - btcPrev5, solAcceleration: sol5 - solPrev5,
    market5: 0.45 * btc5 + 0.55 * sol5,
    marketAcceleration: 0.45 * (btc5 - btcPrev5) + 0.55 * (sol5 - solPrev5)
  };
}

function outcome(rows, i) {
  const entry = rows[i].PUMP.close;
  let max15 = -Infinity, max60 = -Infinity, min60 = Infinity;
  let pmResult = "TIME";
  for (let j = i + 1; j <= Math.min(rows.length - 1, i + 360); j++) {
    const hi = pct(entry, rows[j].PUMP.high), lo = pct(entry, rows[j].PUMP.low);
    if (j <= i + 15) max15 = Math.max(max15, hi);
    if (j <= i + 60) { max60 = Math.max(max60, hi); min60 = Math.min(min60, lo); }
    if (pmResult === "TIME") {
      // Conservative same-candle ordering: loss wins the tie.
      if (lo <= -1.10) pmResult = "LOSS";
      else if (hi >= 2.50) pmResult = "WIN"; // about +2% NET after 0.50% round-trip fee, before spread
    }
  }
  return { max15, max60, min60, pmResult, forward15Positive: max15 >= 0.50 };
}

function candidates(rows) {
  const result = [];
  let lastAt = -Infinity;
  for (let i = 60; i < rows.length - 360; i++) {
    const f = feature(rows, i);
    const bottomShape = f.pump30 <= -0.65 && f.rangePosition <= 0.28 && f.pump3 >= 0.04;
    if (!bottomShape || i - lastAt < 15) continue;
    result.push({ i, t: rows[i].t, ...f, ...outcome(rows, i) });
    lastAt = i;
  }
  return result;
}

const FILTERS = {
  baseline: () => true,
  not_joint_fall: x => !(x.btc5 < -0.08 && x.sol5 < -0.12),
  joint_turn: x => x.marketAcceleration > 0 && (x.btcAcceleration > 0 || x.solAcceleration > 0),
  confirmed_turn: x => x.marketAcceleration > 0.08 && x.market5 > -0.08 && x.solAcceleration > 0,
  synchronized_rise: x => x.btc5 > 0 && x.sol5 > 0,
  sol_leads_turn: x => x.solAcceleration > 0.12 && x.sol5 > -0.10,
  btc_safety_sol_turn: x => x.btc5 > -0.08 && x.solAcceleration > 0.10
};

function score(xs, filter) {
  const chosen = xs.filter(filter);
  const decided = chosen.filter(x => x.pmResult !== "TIME");
  const wins = decided.filter(x => x.pmResult === "WIN").length;
  return {
    candidates: chosen.length, decided: decided.length, wins,
    pmWinRate: decided.length ? wins / decided.length : null,
    forward15HitRate: chosen.length ? chosen.filter(x => x.forward15Positive).length / chosen.length : null,
    medianMax15: median(chosen.map(x => x.max15)), medianMax60: median(chosen.map(x => x.max60)),
    medianMin60: median(chosen.map(x => x.min60))
  };
}

function lagTable(rows) {
  const returns = key => rows.slice(1).map((x, i) => pct(rows[i][key].close, x[key].close));
  const pump = returns("PUMP"), btc = returns("BTC"), sol = returns("SOL");
  const lagCorr = (market, lag) => {
    const a = [], b = [];
    for (let i = Math.max(0, lag); i < pump.length + Math.min(0, lag); i++) {
      a.push(market[i - lag]); b.push(pump[i]);
    }
    return corr(a, b);
  };
  return [-10, -5, -3, -1, 0, 1, 3, 5, 10].map(lag => ({
    lagMinutes: lag, btcToPump: lagCorr(btc, lag), solToPump: lagCorr(sol, lag)
  }));
}

function synthetic() {
  const cases = [
    { name: "joint_bottom_turn", pumpBottom: true, btc5: 0.05, sol5: 0.12, btcAcceleration: 0.18, solAcceleration: 0.31 },
    { name: "pump_false_bounce_market_falls", pumpBottom: true, btc5: -0.18, sol5: -0.30, btcAcceleration: -0.05, solAcceleration: -0.09 },
    { name: "sol_leads_btc_flat", pumpBottom: true, btc5: -0.01, sol5: 0.08, btcAcceleration: 0.02, solAcceleration: 0.24 },
    { name: "late_market_rise_no_pump_bottom", pumpBottom: false, btc5: 0.15, sol5: 0.25, btcAcceleration: 0.10, solAcceleration: 0.18 }
  ];
  return cases.map(x => {
    const market5 = 0.45 * x.btc5 + 0.55 * x.sol5;
    const marketAcceleration = 0.45 * x.btcAcceleration + 0.55 * x.solAcceleration;
    const jointFall = x.btc5 < -0.08 && x.sol5 < -0.12;
    const confirmation = marketAcceleration > 0.08 && market5 > -0.08 && x.solAcceleration > 0;
    return { name: x.name, decision: !x.pumpBottom ? "NO_BOTTOM" : jointFall ? "BLOCK" : confirmation ? "CONFIRM" : "WAIT" };
  });
}

(async () => {
  const end = Math.floor(Date.now() / MINUTE) * MINUTE;
  const start = end - DAYS * 24 * 60 * MINUTE;
  const [PUMP, BTC, SOL] = await Promise.all([
    fetchWeek("PUMPUSDT", start, end), fetchWeek("BTCUSDT", start, end), fetchWeek("SOLUSDT", start, end)
  ]);
  const rows = align({ PUMP, BTC, SOL });
  const cs = candidates(rows);
  const splitAt = rows[0].t + Math.floor((rows[rows.length - 1].t - rows[0].t) * 0.65);
  const train = cs.filter(x => x.t <= splitAt), test = cs.filter(x => x.t > splitAt);
  const filters = Object.fromEntries(Object.entries(FILTERS).map(([name, fn]) => [name, {
    train: score(train, fn), test: score(test, fn)
  }]));
  const report = {
    generatedAt: new Date().toISOString(), days: DAYS,
    interval: { start: new Date(rows[0].t).toISOString(), end: new Date(rows[rows.length - 1].t).toISOString(), minutes: rows.length },
    methodology: { trainFraction: 0.65, bottom: "PUMP 30m <= -0.65%, position in 60m range <= 28%, 3m rebound >= 0.04%, 15m event spacing", pmWin: "+2.50% gross before -1.10%; conservative stop-first; 360m horizon", earlyMove: "+0.50% high within 15m" },
    correlations: lagTable(rows), candidates: { total: cs.length, train: train.length, test: test.length },
    filters, synthetic: synthetic()
  };
  fs.writeFileSync(OUT, JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report, null, 2));
  console.error(`saved ${OUT}`);
})().catch(error => { console.error(error.stack || error); process.exit(1); });
