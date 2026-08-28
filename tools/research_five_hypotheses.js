#!/usr/bin/env node

/* Five fixed, causal hypotheses supplied by Serge. Research-only: no exchange writes. */
const API="https://data-api.binance.vision/api/v3/klines",M=60000,DAYS=Number(process.argv[2]||60),END_ARG=process.argv[3]||null;
const FEE=.0021,SLIP=.0008;
const pct=(a,b)=>(b/a-1)*100,sum=x=>x.reduce((a,b)=>a+b,0);
const net=(e,x)=>((x*(1-FEE))/(e*(1+FEE))-1)*100;
const priceForNet=(e,n)=>e*(1+n/100)*(1+FEE)/(1-FEE);
async function fetchSymbol(symbol,start,end){const out=[];for(let c=start;c<end;){const r=await fetch(`${API}?symbol=${symbol}&interval=1m&startTime=${c}&endTime=${end-1}&limit=1000`);if(!r.ok)throw Error(`${symbol} HTTP ${r.status}`);const p=await r.json();if(!p.length)break;out.push(...p);c=+p[p.length-1][0]+M}return out.map(x=>({t:+x[0],o:+x[1],h:+x[2],l:+x[3],c:+x[4],q:+x[7],bq:+x[10]}))}
function align(s){const m=Object.fromEntries(Object.entries(s).map(([k,v])=>[k,new Map(v.map(x=>[x.t,x]))]));return[...m.P.keys()].filter(t=>m.B.has(t)&&m.S.has(t)).sort((a,b)=>a-b).map(t=>({t,P:m.P.get(t),B:m.B.get(t),S:m.S.get(t)}))}
function tr(a,i,k="P"){const x=a[i][k],p=a[i-1][k].c;return Math.max(x.h-x.l,Math.abs(x.h-p),Math.abs(x.l-p))/p*100}
function atr(a,i,n,k="P"){let s=0;for(let j=i-n+1;j<=i;j++)s+=tr(a,j,k);return s/n}
function atrAverage(a,i,atrLength,averageLength,k="P"){let s=0;for(let j=i-averageLength+1;j<=i;j++)s+=atr(a,j,atrLength,k);return s/averageLength}
function sellDecay(a,i){const avg=(from,to)=>sum(a.slice(i-from,i-to+1).map(x=>x.P.q-x.P.bq))/(from-to+1);return avg(2,0)<=.85*Math.max(avg(9,5),1)}
function redRun(a,i){let best=0,run=0;for(let j=i-10;j<i;j++){run=a[j].P.c<a[j].P.o?run+1:0;best=Math.max(best,run)}return best}
function level60(a,i){return Math.max(...a.slice(i-60,i).map(x=>x.P.h))}
function rangePosition(a,i,n){const s=a.slice(i-n+1,i+1),lo=Math.min(...s.map(x=>x.P.l)),hi=Math.max(...s.map(x=>x.P.h));return hi>lo?(a[i].P.c-lo)/(hi-lo):.5}
function volumeCluster(a,i){const rows=a.slice(i-59,i+1),b=new Map(),width=.0015;for(const x of rows){const p=(x.P.h+x.P.l+x.P.c)/3,k=Math.round(Math.log(p)/width);b.set(k,(b.get(k)||0)+x.P.q)}const total=sum(rows.map(x=>x.P.q)),top=[...b.entries()].sort((x,y)=>y[1]-x[1])[0];if(!top||top[1]<.30*total)return null;return Math.exp(top[0]*width)}

const H={
 quiet_entry:{target:1.5,stop:-.9,entry:"LIMIT",ttl:2,
  features:(a,i)=>({fall30:pct(a[i-30].P.c,a[i].P.c),atrRatio:atr(a,i,5)/atrAverage(a,i,5,20),baseRange:(Math.max(...a.slice(i-2,i+1).map(x=>x.P.h))/Math.min(...a.slice(i-2,i+1).map(x=>x.P.l))-1)*100}),
  accept:f=>f.fall30<=-1.5&&f.atrRatio<=.8&&f.baseRange<=.2},
 fake_breakout:{target:2,stop:-1.1,entry:"LEVEL_LIMIT",ttl:3,
  features:(a,i)=>{const l=level60(a,i);return{touchRatio:a[i].P.h/l,closeBelow:a[i].P.c<l}},
  accept:f=>f.touchRatio>=.999&&f.touchRatio<=1.001&&f.closeBelow,
  level:(a,i)=>level60(a,i)*.998},
 volume_cluster:{target:1.8,stop:-.9,entry:"STOP_LIMIT",ttl:2,
  features:(a,i)=>{const n=volumeCluster(a,i);return{cluster:n!=null,near:n==null?false:Math.abs(pct(n,a[i].P.c))<=.15,fall30:pct(a[i-30].P.c,a[i].P.c),sellDecay:sellDecay(a,i),btc5:pct(a[i-5].B.c,a[i].B.c),sol5:pct(a[i-5].S.c,a[i].S.c)}},
  accept:f=>f.cluster&&f.near&&f.fall30<=-1.2&&f.sellDecay&&f.btc5>-.3&&f.sol5>-.3},
 cross_impulse:{target:1.2,stop:-.8,entry:"MARKET",ttl:0,early:true,
  features:(a,i)=>{const btc1=pct(a[i-1].B.c,a[i].B.c),sol1=pct(a[i-1].S.c,a[i].S.c),pump1=pct(a[i-1].P.c,a[i].P.c);return{btc1,sol1,pump1,leadGap:Math.max(btc1,sol1)-pump1,rangePosition:rangePosition(a,i,30),sellDecay:sellDecay(a,i)}},
  accept:f=>(f.btc1>.15||f.sol1>.2)&&f.pump1<.1&&f.leadGap>=.1&&f.rangePosition<=.5&&f.sellDecay},
 session_rebound:{target:1.5,stop:-.9,entry:"FAST_STOP_LIMIT",ttl:1,time:true,
  features:(a,i)=>{const x=a[i].P,loc=x.h>x.l?(x.c-x.l)/(x.h-x.l):.5;return{hour:new Date(a[i].t).getUTCHours(),trend4h:pct(a[i-240].P.c,x.c),atrRatio:atr(a,i,30)/atrAverage(a,i,30,1440),redRun:redRun(a,i),green:x.c>x.o,strongClose:loc>.5}},
  accept:f=>f.hour>=9&&f.hour<15&&f.trend4h<0&&f.atrRatio>1&&f.redRun>=3&&f.green&&f.strongClose}
};
for(const h of Object.values(H))h.signal=(a,i)=>h.accept(h.features(a,i));
function entry(a,c,h){const i=c.i,ref=a[i].P.c;if(h.entry==="MARKET")return{at:i+1,price:a[i+1].P.o*(1+SLIP)};let limit,trigger=null;if(h.entry==="LIMIT")limit=ref*.999;else if(h.entry==="LEVEL_LIMIT")limit=h.level(a,i);else if(h.entry==="STOP_LIMIT"){trigger=ref*1.001;limit=trigger}else{trigger=ref*1.0005;limit=trigger}let trig=trigger==null;for(let j=i+1;j<=i+h.ttl;j++){const x=a[j].P;if(!trig&&x.h>=trigger){trig=true;if(x.o<=limit)return{at:j,price:trigger};continue}if(trig&&x.l<=limit)return{at:j,price:Math.min(limit,x.o)}}return null}
function exit(a,e,h){const tp=priceForNet(e.price,h.target),sl=priceForNet(e.price,h.stop);let best=-Infinity;for(let j=e.at+1;j<=Math.min(a.length-1,e.at+360);j++){best=Math.max(best,net(e.price,a[j].P.h));if(a[j].P.l<=sl)return{at:j,pnl:net(e.price,Math.min(sl,a[j].P.o)*(1-SLIP)),why:"STOP"};if(a[j].P.h>=tp)return{at:j,pnl:h.target,why:"TP"};if(h.early&&j===e.at+2&&pct(e.price,a[j].P.c)<.2)return{at:j,pnl:net(e.price,a[j].P.c*(1-SLIP)),why:"EARLY"};if(h.time&&j===e.at+20&&best<.5)return{at:j,pnl:net(e.price,a[j].P.c*(1-SLIP)),why:"TIME"}}const j=Math.min(a.length-1,e.at+360);return{at:j,pnl:net(e.price,a[j].P.c*(1-SLIP)),why:"TIMEOUT"}}
function metrics(t,signals,unfilled){const w=t.filter(x=>x.pnl>0),l=t.filter(x=>x.pnl<=0),gw=sum(w.map(x=>x.pnl)),gl=-sum(l.map(x=>x.pnl));let eq=0,peak=0,dd=0;for(const x of t){eq+=x.pnl;peak=Math.max(peak,eq);dd=Math.max(dd,peak-eq)}return{signals,filled:t.length,unfilled,wins:w.length,losses:l.length,winRate:t.length?w.length/t.length:null,averageNet:t.length?sum(t.map(x=>x.pnl))/t.length:null,totalNetPoints:sum(t.map(x=>x.pnl)),grossProfitPoints:gw,grossLossPoints:gl,profitFactor:gl?gw/gl:null,maxSequentialDrawdownPoints:dd,stops:l.filter(x=>x.why==="STOP").length,earlyOrTime:t.filter(x=>x.why==="EARLY"||x.why==="TIME").length}}
function simulate(a,h,from,to){let busy=-1,signals=0,unfilled=0;const t=[];for(let i=Math.max(from,1500);i<to-365;i++){if(i<=busy||!h.signal(a,i))continue;signals++;const e=entry(a,{i},h);if(!e){unfilled++;busy=i+h.ttl;continue}const x=exit(a,e,h);busy=x.at;t.push(x)}return metrics(t,signals,unfilled)}
function synthetic(){const cases=[
 ["quiet_entry","compression after deep fall",{fall30:-1.6,atrRatio:.75,baseRange:.18},true],["quiet_entry","volatility remains wide",{fall30:-1.6,atrRatio:.95,baseRange:.18},false],
 ["fake_breakout","touch and close back below resistance",{touchRatio:1,closeBelow:true},true],["fake_breakout","clean breakout above resistance",{touchRatio:1.002,closeBelow:false},false],
 ["volume_cluster","cluster plus sell decay and safe market",{cluster:true,near:true,fall30:-1.3,sellDecay:true,btc5:-.1,sol5:.05},true],["volume_cluster","cluster without sell decay",{cluster:true,near:true,fall30:-1.3,sellDecay:false,btc5:-.1,sol5:.05},false],
 ["cross_impulse","BTC leads while PUMP lags",{btc1:.2,sol1:.1,pump1:.05,leadGap:.15,rangePosition:.4,sellDecay:true},true],["cross_impulse","PUMP already moved with market",{btc1:.2,sol1:.1,pump1:.16,leadGap:.04,rangePosition:.4,sellDecay:true},false],
 ["session_rebound","EU window red run then strong green",{hour:12,trend4h:-1,atrRatio:1.1,redRun:3,green:true,strongClose:true},true],["session_rebound","same shape outside time window",{hour:18,trend4h:-1,atrRatio:1.1,redRun:3,green:true,strongClose:true},false]
 ];return cases.map(([hypothesis,scenario,features,expected])=>{const actual=H[hypothesis].accept(features);return{hypothesis,scenario,expected,actual,pass:actual===expected}})}
(async()=>{const parsedEnd=END_ARG?Date.parse(END_ARG):Date.now();if(!Number.isFinite(parsedEnd))throw Error(`Invalid end time: ${END_ARG}`);const end=Math.floor(parsedEnd/M)*M,start=end-DAYS*1440*M,[P,B,S]=await Promise.all([fetchSymbol("PUMPUSDT",start,end),fetchSymbol("BTCUSDT",start,end),fetchSymbol("SOLUSDT",start,end)]),a=align({P,B,S}),cut=Math.floor(a.length/2),result={days:DAYS,minutes:a.length,periods:{early:[new Date(a[0].t).toISOString(),new Date(a[cut-1].t).toISOString()],recent:[new Date(a[cut].t).toISOString(),new Date(a[a.length-1].t).toISOString()]},fees:{perSide:FEE,marketSlippage:SLIP},synthetic:synthetic(),results:{}};for(const[n,h]of Object.entries(H))result.results[n]={early:simulate(a,h,0,cut),recent:simulate(a,h,cut,a.length)};console.log(JSON.stringify(result,null,2))})().catch(e=>{console.error(e.stack||e);process.exit(1)});
