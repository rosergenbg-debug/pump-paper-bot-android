#!/usr/bin/env node

/* Causal, candle-level order-type experiment. Research only; never submits an order. */
const API = "https://data-api.binance.vision/api/v3/klines";
const MINUTE = 60_000;
const DAYS = Number(process.argv[2] || 14);
const FEE = 0.0021;              // V6.2 paper fee per side
const SLIPPAGE = 0.0008;         // explicit stress assumption per market fill
const TARGET_NET = 0.0200;
const STOP_NET = -0.0110;

const pct = (a, b) => (b / a - 1) * 100;
const net = (entry, exit) => ((exit * (1 - FEE)) / (entry * (1 + FEE)) - 1) * 100;
const exitForNet = (entry, wanted) => entry * (1 + wanted) * (1 + FEE) / (1 - FEE);

async function candles() {
  const end = Math.floor(Date.now() / MINUTE) * MINUTE;
  const start = end - DAYS * 1440 * MINUTE;
  const out = [];
  for (let cursor = start; cursor < end;) {
    const u = `${API}?symbol=PUMPUSDT&interval=1m&startTime=${cursor}&endTime=${end - 1}&limit=1000`;
    const r = await fetch(u); if (!r.ok) throw new Error(`HTTP ${r.status}`);
    const p = await r.json(); if (!p.length) break;
    out.push(...p); cursor = Number(p[p.length - 1][0]) + MINUTE;
  }
  return out.map(x => ({t:+x[0],o:+x[1],h:+x[2],l:+x[3],c:+x[4]}));
}

function candidateIndices(a) {
  const out=[]; let last=-99;
  for(let i=60;i<a.length-365;i++) {
    const low=Math.min(...a.slice(i-59,i+1).map(x=>x.l));
    const high=Math.max(...a.slice(i-59,i+1).map(x=>x.h));
    const pos=high>low?(a[i].c-low)/(high-low):0.5;
    if(pct(a[i-30].c,a[i].c)<=-0.65 && pos<=0.28 && pct(a[i-3].c,a[i].c)>=0.04 && i-last>=15){out.push(i);last=i;}
  }
  return out;
}

function entry(a,i,type) {
  const ref=a[i].c, ttl=5;
  if(type==="MARKET") return {at:i+1,price:a[i+1].o*(1+SLIPPAGE)};
  if(type==="LIMIT_PULLBACK") {
    const limit=ref*0.9985;
    for(let j=i+1;j<=i+ttl;j++) if(a[j].l<=limit) return {at:j,price:Math.min(limit,a[j].o)};
    return null;
  }
  if(type==="STOP_MARKET_CONFIRM") {
    const stop=ref*1.0015;
    for(let j=i+1;j<=i+ttl;j++) if(a[j].h>=stop) return {at:j,price:Math.max(stop,a[j].o)*(1+SLIPPAGE)};
    return null;
  }
  if(type==="STOP_LIMIT_CONFIRM") {
    const stop=ref*1.0015, limit=ref*1.0025; let triggered=-1;
    for(let j=i+1;j<=i+ttl;j++) {
      if(triggered<0 && a[j].h>=stop) { triggered=j; continue; } // no optimistic same-candle path
      if(triggered>=0 && a[j].l<=limit && a[j].o<=limit) return {at:j,price:Math.min(limit,Math.max(stop,a[j].o))};
    }
    return null;
  }
  throw new Error(type);
}

function bracket(a,e,exitType,trailing=false,targetNet=TARGET_NET,stopNet=STOP_NET) {
  const tp=exitForNet(e.price,targetNet), stop=exitForNet(e.price,stopNet);
  let peak=e.price, activeStop=stop, triggeredAt=-1;
  for(let j=e.at+1;j<=Math.min(a.length-1,e.at+360);j++) {
    peak=Math.max(peak,a[j].h);
    if(trailing && net(e.price,peak)>=0.80) activeStop=Math.max(activeStop,exitForNet(e.price,0.0010));
    if(trailing && net(e.price,peak)>=1.25) activeStop=Math.max(activeStop,peak*0.9945);
    // Conservative candle ordering.
    if(a[j].l<=activeStop) {
      if(exitType==="STOP_MARKET") {
        const px=Math.min(activeStop,a[j].o)*(1-SLIPPAGE);
        return {result:net(e.price,px)>=0?"WIN":"LOSS",pnl:net(e.price,px),exitFailure:false};
      }
      // Stop-limit sell: 0.15% protection band. A fast gap can leave the position open.
      const limit=activeStop*0.9985; triggeredAt=j;
      for(let k=j+1;k<=Math.min(a.length-1,j+3);k++) {
        if(a[k].h>=limit && a[k].o>=limit) {
          const pnl=net(e.price,Math.max(limit,a[k].o));
          return {result:pnl>=0?"WIN":"LOSS",pnl,exitFailure:false};
        }
      }
      const emergency=a[Math.min(a.length-1,j+3)].c*(1-SLIPPAGE);
      return {result:"LOSS",pnl:net(e.price,emergency),exitFailure:true};
    }
    if(a[j].h>=tp) return {result:"WIN",pnl:net(e.price,tp),exitFailure:false};
  }
  const px=a[Math.min(a.length-1,e.at+360)].c*(1-SLIPPAGE);
  return {result:net(e.price,px)>=0?"WIN":"LOSS",pnl:net(e.price,px),exitFailure:false,timeout:true};
}

function syntheticOrderCases() {
  const entryPrice=1.0;
  const stop=exitForNet(entryPrice,STOP_NET);
  const tp=exitForNet(entryPrice,TARGET_NET);
  const marketGapExit=0.975*(1-SLIPPAGE);
  const limitPullback=0.9985;
  return [
    {name:"clean_continuation",marketEntry:"FILLED",stopMarketConfirm:"FILLED_AFTER_CONFIRMATION",limitPullback:"MAY_MISS",lesson:"stop-buy confirms continuation; pullback limit can miss the winner"},
    {name:"pullback_then_rise",marketEntryNetAtTp:net(entryPrice,tp),limitEntryPrice:limitPullback,limitEntryNetAtSameExit:net(limitPullback,tp),lesson:"buy-limit improves entry when a real pullback occurs"},
    {name:"trigger_spike_then_reversal",stopMarketEntry:"FILLED_NEAR_SPIKE",stopLimitEntry:"CAN_REMAIN_UNFILLED",lesson:"confirmation entry avoids weak non-breakouts but can buy a false breakout"},
    {name:"normal_noise_hits_tight_stop_then_rises",stopTriggerPrice:stop,stopMarket:"EXECUTES_LOSS",laterRecovery:"MISSED",lesson:"a closer stop reduces one loss but increases whipsaw exits"},
    {name:"gap_through_stop",plannedStopNet:STOP_NET*100,stopMarketActualNet:net(entryPrice,marketGapExit),stopLimit:"CAN_REMAIN_OPEN",lesson:"stop-market prioritises exit, not price; stop-limit prioritises price, not exit"},
    {name:"oco_take_profit",takeProfitPrice:tp,behaviour:"TP fill cancels stop; stop fill cancels TP",lesson:"prevents contradictory live exit orders but does not improve signal quality"}
  ];
}

function run(a,indices,spec,targetNet=TARGET_NET,stopNet=STOP_NET) {
  const trades=[]; let unfilled=0;
  for(const i of indices) {
    const e=entry(a,i,spec.entry); if(!e){unfilled++;continue;}
    trades.push(bracket(a,e,spec.exit,spec.trailing,targetNet,stopNet));
  }
  const wins=trades.filter(x=>x.pnl>0).length;
  const losses=trades.length-wins;
  const sum=trades.reduce((s,x)=>s+x.pnl,0);
  return {signals:indices.length,filled:trades.length,unfilled,fillRate:trades.length/indices.length,
    wins,losses,winRate:trades.length?wins/trades.length:null,averageNet:trades.length?sum/trades.length:null,
    totalNetPoints:sum,stopLimitExitFailures:trades.filter(x=>x.exitFailure).length,
    timeouts:trades.filter(x=>x.timeout).length};
}

function bands(a,indices) {
  const groups={fall_065_100:[],fall_100_150:[],fall_over_150:[],bottom_0_10:[],bottom_10_20:[],bottom_20_28:[],rebound_004_012:[],rebound_012_030:[],rebound_over_030:[]};
  for(const i of indices) {
    const fall=pct(a[i-30].c,a[i].c);
    const low=Math.min(...a.slice(i-59,i+1).map(x=>x.l));
    const high=Math.max(...a.slice(i-59,i+1).map(x=>x.h));
    const pos=(a[i].c-low)/(high-low);
    const rebound=pct(a[i-3].c,a[i].c);
    groups[fall>-1.0?"fall_065_100":fall>-1.5?"fall_100_150":"fall_over_150"].push(i);
    groups[pos<=0.10?"bottom_0_10":pos<=0.20?"bottom_10_20":"bottom_20_28"].push(i);
    groups[rebound<0.12?"rebound_004_012":rebound<0.30?"rebound_012_030":"rebound_over_030"].push(i);
  }
  return groups;
}

(async()=>{
 const a=await candles(), ids=candidateIndices(a), split=Math.floor(a.length*0.65);
 const train=ids.filter(i=>i<=split), test=ids.filter(i=>i>split);
 const specs={
  market_oco:{entry:"MARKET",exit:"STOP_MARKET"},
  pullback_limit_oco:{entry:"LIMIT_PULLBACK",exit:"STOP_MARKET"},
  confirmation_stop_market_oco:{entry:"STOP_MARKET_CONFIRM",exit:"STOP_MARKET"},
  confirmation_stop_limit_oco:{entry:"STOP_LIMIT_CONFIRM",exit:"STOP_MARKET"},
  market_trailing_oco:{entry:"MARKET",exit:"STOP_MARKET",trailing:true},
  market_stop_limit_exit:{entry:"MARKET",exit:"STOP_LIMIT"}
 };
 const result={days:DAYS,minutes:a.length,candidates:ids.length,assumptions:{feePerSide:FEE,marketSlippagePerFill:SLIPPAGE,targetNet:TARGET_NET,stopNet:STOP_NET,entryTtlMinutes:5},synthetic:syntheticOrderCases(),results:{},targetGrid:{fixedStop:{},matchedRisk:{}},marketSections:{},deepFallOrderGrid:{}};
 for(const [k,s] of Object.entries(specs)) result.results[k]={train:run(a,train,s),test:run(a,test,s)};
 const targets=[0.01,0.015,0.02,0.03];
 const matchedStops={"0.01":-0.0075,"0.015":-0.009,"0.02":-0.011,"0.03":-0.013};
 for(const target of targets) {
   const key=String(target);
   result.targetGrid.fixedStop[key]={}; result.targetGrid.matchedRisk[key]={};
   for(const order of ["market_oco","pullback_limit_oco","confirmation_stop_limit_oco"]) {
     result.targetGrid.fixedStop[key][order]=run(a,test,specs[order],target,STOP_NET);
     result.targetGrid.matchedRisk[key][order]=run(a,test,specs[order],target,matchedStops[key]);
   }
 }
 const trainBands=bands(a,train), testBands=bands(a,test);
 for(const [name,section] of Object.entries(testBands)) {
   result.marketSections[name]={};
   for(const target of targets) result.marketSections[name][String(target)]={
     train:run(a,trainBands[name],specs.market_oco,target,matchedStops[String(target)]),
     test:run(a,section,specs.market_oco,target,matchedStops[String(target)])
   };
 }
 for(const target of targets) {
   const key=String(target); result.deepFallOrderGrid[key]={};
   for(const order of ["market_oco","pullback_limit_oco","confirmation_stop_limit_oco"]) {
     result.deepFallOrderGrid[key][order]={
       train:run(a,trainBands.fall_over_150,specs[order],target,matchedStops[key]),
       test:run(a,testBands.fall_over_150,specs[order],target,matchedStops[key])
     };
   }
 }
 console.log(JSON.stringify(result,null,2));
})().catch(e=>{console.error(e.stack||e);process.exit(1)});
