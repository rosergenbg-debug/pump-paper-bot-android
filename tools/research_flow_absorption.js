#!/usr/bin/env node

/* Research-only causal replay of flow/absorption hypotheses. No exchange writes. */
const API="https://data-api.binance.vision/api/v3/klines", M=60000, DAYS=Number(process.argv[2]||30);
const FEE=0.0021, SLIP=0.0008;
const pct=(a,b)=>(b/a-1)*100, sum=a=>a.reduce((x,y)=>x+y,0), clamp=(x,a,b)=>Math.max(a,Math.min(b,x));
const net=(e,x)=>((x*(1-FEE))/(e*(1+FEE))-1)*100;
const priceForNet=(e,n)=>e*(1+n/100)*(1+FEE)/(1-FEE);

async function fetchSymbol(symbol,start,end){const out=[];for(let c=start;c<end;){const u=`${API}?symbol=${symbol}&interval=1m&startTime=${c}&endTime=${end-1}&limit=1000`;const r=await fetch(u);if(!r.ok)throw Error(`${symbol} HTTP ${r.status}`);const p=await r.json();if(!p.length)break;out.push(...p);c=+p[p.length-1][0]+M}return out.map(x=>({t:+x[0],o:+x[1],h:+x[2],l:+x[3],c:+x[4],q:+x[7],bq:+x[10]}))}
function align(s){const maps=Object.fromEntries(Object.entries(s).map(([k,v])=>[k,new Map(v.map(x=>[x.t,x]))]));return [...maps.P.keys()].filter(t=>maps.B.has(t)&&maps.S.has(t)).sort((a,b)=>a-b).map(t=>({t,P:maps.P.get(t),B:maps.B.get(t),S:maps.S.get(t)}))}
function window(a,i,from,to){return a.slice(i-from,i-to+1)}
function share(xs){const q=sum(xs.map(x=>x.P.q));return q?100*sum(xs.map(x=>x.P.bq))/q:50}
function delta(xs){const q=sum(xs.map(x=>x.P.q));return q?100*sum(xs.map(x=>2*x.P.bq-x.P.q))/q:0}
function feature(a,i){
 const recent=window(a,i,2,0), prior5=window(a,i,9,5), prior15=window(a,i,24,10);
 const q3=sum(recent.map(x=>x.P.q))/3,q5=sum(prior5.map(x=>x.P.q))/5,q15=sum(prior15.map(x=>x.P.q))/15;
 const sell3=sum(recent.map(x=>x.P.q-x.P.bq))/3,sell5=sum(prior5.map(x=>x.P.q-x.P.bq))/5;
 const buy3=share(recent),buy5=share(prior5),d3=delta(recent),d5=delta(prior5);
 const priorLow=Math.min(...prior5.map(x=>x.P.l)),recentLow=Math.min(...recent.map(x=>x.P.l));
 const lowHeld=recentLow>=priorLow*0.999;
 const pump5=pct(a[i-5].P.c,a[i].P.c), market5=.45*pct(a[i-5].B.c,a[i].B.c)+.55*pct(a[i-5].S.c,a[i].S.c);
 const price3=pct(a[i-3].P.c,a[i].P.c);
 const absorption=buy5<=47&&price3>=-.05&&lowHeld;
 const sellDecay=sell5>0&&sell3/sell5<=.85;
 const buyRecovery=buy3>=49&&buy3-buy5>=4;
 const deltaTurn=d3-d5>=8&&d3>=-2;
 const volumeSpike=q3>=1.2*Math.max(q15,1);
 const relativeStrength=pump5-market5>=.12;
 const marketSafe=!(pct(a[i-5].B.c,a[i].B.c)<-.08&&pct(a[i-5].S.c,a[i].S.c)<-.12);
 const sellActivity=sell3/Math.max(q15,1),hardAbsorption=sellActivity>=.65&&price3>=-.03&&lowHeld;
 const buyResponse=buyRecovery&&price3>=.08;
 const bar=a[i].P,closeLocation=bar.h>bar.l?(bar.c-bar.l)/(bar.h-bar.l):.5;
 const priorRed=[a[i-1].P,a[i-2].P,a[i-3].P].filter(x=>x.c<x.o).length>=2;
 const bounceSize=Math.max(bar.c-a[i-3].P.c,0),holdFloor=bar.c-.60*bounceSize;
 const bounceConfirmed=bar.c>bar.o&&closeLocation>=.65&&priorRed&&a[i+1].P.l>=holdFloor&&a[i+2].P.l>=holdFloor;
 const score=(hardAbsorption?2:absorption?1:0)+(sellDecay?1:0)+(buyResponse?2:buyRecovery?1:0)+(deltaTurn?2:0)+(volumeSpike?1:0)+(relativeStrength?1:0)+(lowHeld?1:0)+(marketSafe?1:0);
 const hour=new Date(a[i].t).getUTCHours(),context4h=i>=240?pct(a[i-240].P.c,a[i].P.c):0,projectedAtr=1.5*atrPct(a,i)*Math.sqrt(20);
 const btc30=pct(a[i-30].B.c,a[i].B.c),sol30=pct(a[i-30].S.c,a[i].S.c);
 const regime=btc30<=-.5&&sol30<=-.8?"RISK_OFF":btc30>=.3&&sol30>=.5?"RISK_ON":"NEUTRAL";
 const vp=new Map();for(let j=Math.max(0,i-239);j<=i;j++){const price=(a[j].P.h+a[j].P.l+a[j].P.c)/3,b=Math.round(Math.log(price)/.001);vp.set(b,(vp.get(b)||0)+a[j].P.q)}
 const nodeBucket=[...vp.entries()].sort((x,y)=>y[1]-x[1])[0]?.[0],volumeNode=nodeBucket==null?bar.c:Math.exp(nodeBucket*.001),nearVolumeNode=Math.abs(pct(volumeNode,bar.c))<=.20;
 const range30=pct(Math.min(...a.slice(i-29,i+1).map(x=>x.P.l)),Math.max(...a.slice(i-29,i+1).map(x=>x.P.h)));
 return {absorption,hardAbsorption,sellDecay,buyRecovery,buyResponse,deltaTurn,volumeSpike,relativeStrength,lowHeld,marketSafe,bounceConfirmed,score,buy3,buy5,d3,d5,sellRatio:sell5?sell3/sell5:1,sellActivity,price3,pump5,market5,hour,context4h,projectedAtr,regime,nearVolumeNode,range30};
}
function candidates(a){const out=[];let last=-99;for(let i=60;i<a.length-365;i++){const lo=Math.min(...a.slice(i-59,i+1).map(x=>x.P.l)),hi=Math.max(...a.slice(i-59,i+1).map(x=>x.P.h));const drop=pct(a[i-30].P.c,a[i].P.c),pos=(a[i].P.c-lo)/(hi-lo),r3=pct(a[i-3].P.c,a[i].P.c);if(drop<=-.65&&pos<=.28&&r3>=.04&&i-last>=15){out.push({i,drop,pos,r3,...feature(a,i)});last=i}}return out}
function enter(a,c,delay=0){const base=c.i+delay,ref=a[base].P.c,stop=ref*1.0015,limit=ref*1.0025;let trig=false;for(let j=base+1;j<=base+5;j++){if(!trig&&a[j].P.h>=stop){trig=true;continue}if(trig&&a[j].P.l<=limit&&a[j].P.o<=limit)return{at:j,price:Math.min(limit,Math.max(stop,a[j].P.o))}}return null}
function atrPct(a,i){let s=0;for(let j=i-13;j<=i;j++){const prev=a[j-1].P.c;s+=Math.max(a[j].P.h-a[j].P.l,Math.abs(a[j].P.h-prev),Math.abs(a[j].P.l-prev))/prev*100}return s/14}
function exit(a,e,profile,c){
 let target=2,stop=-1.1,timeStop=0;
 if(profile==="DYNAMIC_ATR"){const projected=1.5*atrPct(a,e.at)*Math.sqrt(20);target=clamp(projected,.9,2.2);stop=-clamp(target*.55,.70,1.15);timeStop=25}
 if(profile==="DYNAMIC_RANGE"){target=clamp(.70*c.range30-.42,.9,2.2);stop=-clamp(target*.55,.70,1.15);timeStop=35}
 if(profile==="FIXED_TIME"){timeStop=25}
 const tp=priceForNet(e.price,target),sl=priceForNet(e.price,stop);let best=-Infinity;
 for(let j=e.at+1;j<=Math.min(a.length-1,e.at+360);j++){
   best=Math.max(best,net(e.price,a[j].P.h));
   if(a[j].P.l<=sl){const px=Math.min(sl,a[j].P.o)*(1-SLIP);return{at:j,pnl:net(e.price,px),target,stop,why:"STOP"}}
   if(a[j].P.h>=tp)return{at:j,pnl:target,target,stop,why:"TP"};
   if(timeStop&&j>=e.at+timeStop&&best<.70){const px=a[j].P.c*(1-SLIP);return{at:j,pnl:net(e.price,px),target,stop,why:"TIME"}}
 }
 const j=Math.min(a.length-1,e.at+360);return{at:j,pnl:net(e.price,a[j].P.c*(1-SLIP)),target,stop,why:"TIMEOUT"}
}
function partialExit(a,e){let peak=e.price,partial=false,realized=0;const initialStop=priceForNet(e.price,-1.1),partialPrice=priceForNet(e.price,1),finalTarget=priceForNet(e.price,2.5),protected=priceForNet(e.price,.10);for(let j=e.at+1;j<=Math.min(a.length-1,e.at+360);j++){peak=Math.max(peak,a[j].P.h);const trail=partial?Math.max(protected,peak*.9945):initialStop;if(a[j].P.l<=trail){const p=net(e.price,Math.min(trail,a[j].P.o)*(1-SLIP));return{at:j,pnl:partial?.5*realized+.5*p:p,target:partial?1.75:2,stop:-1.1,why:"STOP"}}if(!partial&&a[j].P.h>=partialPrice){partial=true;realized=1}if(partial&&a[j].P.h>=finalTarget)return{at:j,pnl:.5*realized+.5*2.5,target:1.75,stop:-1.1,why:"TP"};if(j>=e.at+40&&!partial){const p=net(e.price,a[j].P.c*(1-SLIP));return{at:j,pnl:p,target:1.75,stop:-1.1,why:"TIME"}}}const j=Math.min(a.length-1,e.at+360);return{at:j,pnl:net(e.price,a[j].P.c*(1-SLIP)),target:1.75,stop:-1.1,why:"TIMEOUT"}}
const FILTERS={baseline:{fn:x=>true},hard_price_impact:{fn:x=>x.hardAbsorption&&x.buyResponse},bounce_quality:{fn:x=>x.bounceConfirmed,delay:2},bounce_flow:{fn:x=>x.bounceConfirmed&&x.sellDecay&&x.buyResponse,delay:2},volume_node:{fn:x=>x.nearVolumeNode},volume_node_flow:{fn:x=>x.nearVolumeNode&&x.score>=3},regime_policy:{fn:x=>x.regime==="RISK_OFF"?(x.hardAbsorption&&x.score>=5):x.regime==="RISK_ON"?(x.relativeStrength&&x.pos<=.20&&x.score>=3):(x.bounceConfirmed&&x.volumeSpike&&x.score>=3),delay:2},absorption:{fn:x=>x.absorption},buy_recovery:{fn:x=>x.buyRecovery},sell_decay:{fn:x=>x.sellDecay},delta_turn:{fn:x=>x.deltaTurn},volume_rebound:{fn:x=>x.volumeSpike&&x.buyRecovery},relative_strength:{fn:x=>x.relativeStrength},relative_sell_decay:{fn:x=>x.relativeStrength&&x.sellDecay},flow_score_3:{fn:x=>x.score>=3},flow_score_4:{fn:x=>x.score>=4},flow_score_5:{fn:x=>x.score>=5},deep_flow_4:{fn:x=>x.drop<=-1.5&&x.score>=4},deep_absorption:{fn:x=>x.drop<=-1.5&&x.absorption&&x.buyRecovery},session_asia:{fn:x=>x.hour<8},session_europe:{fn:x=>x.hour>=8&&x.hour<16},session_america:{fn:x=>x.hour>=16},context_4h_down:{fn:x=>x.context4h<0},context_4h_up:{fn:x=>x.context4h>=0},high_volatility:{fn:x=>x.projectedAtr>=1.2}};
function metrics(trades,signals,unfilled){const wins=trades.filter(x=>x.pnl>0),loss=trades.filter(x=>x.pnl<=0),grossWin=sum(wins.map(x=>x.pnl)),grossLoss=-sum(loss.map(x=>x.pnl));let equity=0,peak=0,maxDd=0;for(const t of trades){equity+=t.pnl;peak=Math.max(peak,equity);maxDd=Math.max(maxDd,peak-equity)}return{signals,filled:trades.length,unfilled,winRate:trades.length?wins.length/trades.length:null,averageNet:trades.length?sum(trades.map(x=>x.pnl))/trades.length:null,totalNetPoints:sum(trades.map(x=>x.pnl)),profitFactor:grossLoss?grossWin/grossLoss:null,maxSequentialDrawdownPoints:maxDd,stops:trades.filter(x=>x.why==="STOP").length,timeExits:trades.filter(x=>x.why==="TIME").length}}
function simulate(a,cs,filter,profile){let busy=-1,signals=0,unfilled=0;const trades=[];for(const c of cs){if(c.i<=busy||!filter.fn(c))continue;signals++;const e=enter(a,c,filter.delay||0);if(!e){unfilled++;continue}const x=profile==="PARTIAL"?partialExit(a,e):exit(a,e,profile,c);busy=x.at;trades.push(x)}return metrics(trades,signals,unfilled)}
(async()=>{const end=Math.floor(Date.now()/M)*M,start=end-DAYS*1440*M;const [P,B,S]=await Promise.all([fetchSymbol("PUMPUSDT",start,end),fetchSymbol("BTCUSDT",start,end),fetchSymbol("SOLUSDT",start,end)]);const a=align({P,B,S}),cs=candidates(a),cut=a[0].t+.65*(a[a.length-1].t-a[0].t),train=cs.filter(x=>x.t?x.t<=cut:a[x.i].t<=cut),test=cs.filter(x=>a[x.i].t>cut);const result={days:DAYS,minutes:a.length,candidates:{all:cs.length,train:train.length,test:test.length},fees:{perSide:FEE,marketSlippage:SLIP},results:{}};for(const [name,f]of Object.entries(FILTERS)){result.results[name]={};for(const p of ["FIXED","FIXED_TIME","DYNAMIC_ATR","DYNAMIC_RANGE","PARTIAL"])result.results[name][p]={train:simulate(a,train,f,p),test:simulate(a,test,f,p)}}console.log(JSON.stringify(result,null,2))})().catch(e=>{console.error(e.stack||e);process.exit(1)});
