#!/usr/bin/env node

/* Five fixed contextual PUMP hypotheses. Research-only; no exchange writes. */
const API = "https://data-api.binance.vision/api/v3/klines";
const M = 60_000;
const DAYS = Number(process.argv[2] || 120);
const END = Date.parse(process.argv[3] || "2026-08-28T07:55:00Z");
const FEE = 0.0021;
const SLIP = 0.0008;
const pct = (a, b) => (b / a - 1) * 100;
const sum = xs => xs.reduce((a, b) => a + b, 0);
const avg = xs => xs.length ? sum(xs) / xs.length : 0;
const net = (e, x) => ((x * (1 - FEE)) / (e * (1 + FEE)) - 1) * 100;
const priceForNet = (e, n) => e * (1 + n / 100) * (1 + FEE) / (1 - FEE);

async function fetchSymbol(symbol, start, end) {
  const out = [];
  for (let cursor = start; cursor < end;) {
    const url = `${API}?symbol=${symbol}&interval=1m&startTime=${cursor}&endTime=${end - 1}&limit=1000`;
    const response = await fetch(url);
    if (!response.ok) throw Error(`${symbol} HTTP ${response.status}`);
    const page = await response.json();
    if (!page.length) break;
    out.push(...page);
    cursor = +page[page.length - 1][0] + M;
  }
  return out.map(x => ({t:+x[0],o:+x[1],h:+x[2],l:+x[3],c:+x[4],v:+x[5],q:+x[7],n:+x[8],bq:+x[10]}));
}

function align(series) {
  const maps = Object.fromEntries(Object.entries(series).map(([k, v]) => [k, new Map(v.map(x => [x.t, x]))]));
  return [...maps.P.keys()].filter(t => maps.B.has(t) && maps.S.has(t)).sort((a,b)=>a-b)
    .map(t => ({t, P:maps.P.get(t), B:maps.B.get(t), S:maps.S.get(t)}));
}

function efficiency(a, i, n) {
  const direction = Math.abs(a[i].P.c - a[i-n].P.c);
  let path = 0;
  for (let j=i-n+1; j<=i; j++) path += Math.abs(a[j].P.c - a[j-1].P.c);
  return path ? direction / path : 0;
}
function rangePosition(a, i, n) {
  const rows=a.slice(i-n+1,i+1), lo=Math.min(...rows.map(x=>x.P.l)), hi=Math.max(...rows.map(x=>x.P.h));
  return hi>lo ? (a[i].P.c-lo)/(hi-lo) : .5;
}
function sellDecay(a,i) {
  const sells=(from,to)=>avg(a.slice(i-from,i-to+1).map(x=>x.P.q-x.P.bq));
  return sells(2,0) <= .85*Math.max(sells(9,5),1);
}
function buyShare(x) { return x.P.q ? x.P.bq/x.P.q : .5; }
function rollingVwap(a,i,n) {
  const rows=a.slice(i-n+1,i+1), q=sum(rows.map(x=>x.P.q));
  return q ? sum(rows.map(x=>((x.P.h+x.P.l+x.P.c)/3)*x.P.q))/q : a[i].P.c;
}
function volumeNode(a,i,n=240) {
  const rows=a.slice(i-n+1,i+1), width=.0015, bins=new Map();
  for(const x of rows){const p=(x.P.h+x.P.l+x.P.c)/3,k=Math.round(Math.log(p)/width);bins.set(k,(bins.get(k)||0)+x.P.q)}
  const top=[...bins.entries()].sort((x,y)=>y[1]-x[1])[0];
  return top ? Math.exp(top[0]*width) : a[i].P.c;
}
function relativeStrength(a,i,n=15) {
  return pct(a[i-n].P.c,a[i].P.c) - .5*(pct(a[i-n].B.c,a[i].B.c)+pct(a[i-n].S.c,a[i].S.c));
}

const H = {
  regime_adaptive: {
    target:1.2, stop:-.8, ttl:2,
    features:(a,i)=>{const er=efficiency(a,i,20),ret20=pct(a[i-20].P.c,a[i].P.c),pos=rangePosition(a,i,30),green=a[i].P.c>a[i].P.o,pullback=a[i].P.c<a[i-1].P.c;return{er,ret20,pos,green,pullback}},
    accept:f=>(f.er<=.20&&f.pos<=.18&&f.green)||(f.er>=.35&&f.ret20>=.30&&f.pullback&&f.pos>=.45&&f.pos<=.75),
    entry:(a,i)=>({kind:"LIMIT",price:a[i].P.c*.999,ttl:2})
  },
  liquidity_sweep: {
    target:1.0, stop:-.7, ttl:2,
    features:(a,i)=>{const low=Math.min(...a.slice(i-30,i).map(x=>x.P.l)),x=a[i].P,bodyLow=Math.min(x.o,x.c),wick=bodyLow>x.l?(bodyLow-x.l)/(x.h-x.l||1):0;return{low,swept:x.l<=low*.999,closedBack:x.c>low,green:x.c>x.o,wick}},
    accept:f=>f.swept&&f.closedBack&&f.green&&f.wick>=.35,
    entry:(a,i,f)=>({kind:"LIMIT",price:f.low,ttl:2})
  },
  vwap_reversion: {
    target:null, stop:-.8, ttl:2, maxHold:90,
    features:(a,i)=>{const vwap=rollingVwap(a,i,60);return{vwap,deviation:pct(vwap,a[i].P.c),green:a[i].P.c>a[i].P.o,buyShare:buyShare(a[i]),buySharePrev:buyShare(a[i-1])}},
    accept:f=>f.deviation<=-.40&&f.green&&f.buyShare>=.50&&f.buyShare>f.buySharePrev,
    entry:(a,i)=>({kind:"LIMIT",price:a[i].P.c*.999,ttl:2}),
    targetPrice:(e,f)=>Math.max(e.price*1.001,f.vwap)
  },
  sell_exhaustion: {
    target:1.3, stop:-.8, ttl:2,
    features:(a,i)=>{const sells=a.slice(i-20,i).map(x=>x.P.q-x.P.bq),sell=a[i].P.q-a[i].P.bq,priorLow=Math.min(...a.slice(i-10,i).map(x=>x.P.l));return{sellRatio:sell/Math.max(avg(sells),1),holdsLow:a[i].P.l>=priorLow*.9995,green:a[i].P.c>a[i].P.o,buyRecovery:buyShare(a[i])>buyShare(a[i-1])}},
    accept:f=>f.sellRatio>=1.5&&f.holdsLow&&f.green&&f.buyRecovery,
    entry:(a,i)=>({kind:"STOP_LIMIT",price:a[i].P.h*1.0005,ttl:2})
  },
  node_relative_strength: {
    target:1.5, stop:-.9, ttl:2,
    features:(a,i)=>{const node=volumeNode(a,i),hour=new Date(a[i].t).getUTCHours();return{node,near:Math.abs(pct(node,a[i].P.c))<=.20,rs:relativeStrength(a,i),sellDecay:sellDecay(a,i),eu:hour>=8&&hour<16}},
    accept:f=>f.near&&f.rs>=.20&&f.sellDecay&&f.eu,
    entry:(a,i)=>({kind:"STOP_LIMIT",price:a[i].P.h*1.0005,ttl:2})
  }
};

function fill(a,i,order) {
  for(let j=i+1;j<=i+order.ttl;j++){
    const x=a[j].P;
    if(order.kind==="LIMIT"&&x.l<=order.price)return{at:j,price:Math.min(order.price,x.o)};
    if(order.kind==="STOP_LIMIT"&&x.h>=order.price&&x.o<=order.price)return{at:j,price:order.price};
  }
  return null;
}
function closeTrade(a,e,h,f) {
  const tp=h.targetPrice?h.targetPrice(e,f):priceForNet(e.price,h.target), sl=priceForNet(e.price,h.stop), end=Math.min(a.length-1,e.at+(h.maxHold||360));
  for(let j=e.at+1;j<=end;j++){
    const x=a[j].P;
    if(x.l<=sl)return{at:j,pnl:net(e.price,Math.min(sl,x.o)*(1-SLIP)),why:"STOP"};
    if(x.h>=tp)return{at:j,pnl:net(e.price,tp),why:"TP"};
  }
  return{at:end,pnl:net(e.price,a[end].P.c*(1-SLIP)),why:"TIME"};
}
function metrics(trades,signals,unfilled) {
  const wins=trades.filter(x=>x.pnl>0),losses=trades.filter(x=>x.pnl<=0),gp=sum(wins.map(x=>x.pnl)),gl=-sum(losses.map(x=>x.pnl));
  let equity=0,peak=0,dd=0;for(const x of trades){equity+=x.pnl;peak=Math.max(peak,equity);dd=Math.max(dd,peak-equity)}
  return{signals,filled:trades.length,unfilled,wins:wins.length,losses:losses.length,winRate:trades.length?wins.length/trades.length:null,averageNet:trades.length?sum(trades.map(x=>x.pnl))/trades.length:null,totalNetPoints:sum(trades.map(x=>x.pnl)),profitFactor:gl?gp/gl:null,maxSequentialDrawdownPoints:dd};
}
function simulate(a,h,from,to){let busy=-1,signals=0,unfilled=0;const trades=[];for(let i=Math.max(from,1500);i<to-365;i++){if(i<=busy)continue;const f=h.features(a,i);if(!h.accept(f))continue;signals++;const e=fill(a,i,h.entry(a,i,f));if(!e){unfilled++;busy=i+h.ttl;continue}const x=closeTrade(a,e,h,f);trades.push(x);busy=x.at}return metrics(trades,signals,unfilled)}
function synthetic(){const cases=[
  ["regime_adaptive",{er:.15,ret20:-.2,pos:.1,green:true,pullback:false},true],["regime_adaptive",{er:.27,ret20:.1,pos:.5,green:true,pullback:false},false],
  ["liquidity_sweep",{swept:true,closedBack:true,green:true,wick:.5},true],["liquidity_sweep",{swept:true,closedBack:false,green:false,wick:.5},false],
  ["vwap_reversion",{deviation:-.5,green:true,buyShare:.55,buySharePrev:.45},true],["vwap_reversion",{deviation:-.2,green:true,buyShare:.55,buySharePrev:.45},false],
  ["sell_exhaustion",{sellRatio:1.7,holdsLow:true,green:true,buyRecovery:true},true],["sell_exhaustion",{sellRatio:1.7,holdsLow:false,green:true,buyRecovery:true},false],
  ["node_relative_strength",{near:true,rs:.3,sellDecay:true,eu:true},true],["node_relative_strength",{near:true,rs:.1,sellDecay:true,eu:true},false]
  ];return cases.map(([name,f,expected])=>({name,expected,actual:H[name].accept(f),pass:H[name].accept(f)===expected}))}

(async()=>{
  if(!Number.isFinite(END))throw Error("Invalid end time");
  const start=END-DAYS*1440*M;
  const [P,B,S]=await Promise.all([fetchSymbol("PUMPUSDT",start,END),fetchSymbol("BTCUSDT",start,END),fetchSymbol("SOLUSDT",start,END)]);
  const a=align({P,B,S}),cut=Math.floor(a.length/2),result={days:DAYS,minutes:a.length,periods:{early:[new Date(a[0].t).toISOString(),new Date(a[cut-1].t).toISOString()],control:[new Date(a[cut].t).toISOString(),new Date(a[a.length-1].t).toISOString()]},costs:{feePerSide:FEE,marketExitSlippage:SLIP},synthetic:synthetic(),results:{}};
  for(const [name,h] of Object.entries(H))result.results[name]={early:simulate(a,h,0,cut),control:simulate(a,h,cut,a.length)};
  console.log(JSON.stringify(result,null,2));
})().catch(e=>{console.error(e.stack||e);process.exit(1)});
