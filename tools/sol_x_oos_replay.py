#!/usr/bin/env python3
import csv, io, json, math, urllib.request, zipfile
from collections import defaultdict
from datetime import datetime, timezone

SYMBOL='SOLUSDT'; MONTH='2026-02'; FEE=0.0021; LIMIT_DISCOUNT=0.001; TTL=2
TP_NET=0.025; STOP_NET=-0.012; MAX_HOLD=120; MAX_PER_DAY=2; SLIP=0.0008
URL=f'https://data.binance.vision/data/spot/monthly/klines/{SYMBOL}/1m/{SYMBOL}-1m-{MONTH}.zip'

def net_ret(entry, exitp):
    return exitp*(1-FEE)/(entry*(1+FEE))-1

def price_for_net(entry, r):
    return entry*(1+FEE)*(1+r)/(1-FEE)

def ts_dt(v):
    v=int(v)
    sec=v/1_000_000 if v>10**14 else v/1000
    return datetime.fromtimestamp(sec, tz=timezone.utc)

def load():
    raw=urllib.request.urlopen(URL, timeout=60).read()
    z=zipfile.ZipFile(io.BytesIO(raw)); name=z.namelist()[0]
    out=[]
    with z.open(name) as f:
        for r in csv.reader(io.TextIOWrapper(f, encoding='utf-8')):
            out.append({
                't':int(r[0]),'dt':ts_dt(r[0]),'o':float(r[1]),'h':float(r[2]),'l':float(r[3]),'c':float(r[4]),
                'vol':float(r[5]),'q':float(r[7]),'tb':float(r[9]),'tbq':float(r[10])
            })
    return out

def signal(rows,i):
    if i<719: return False, None, None
    w=rows[i-59:i+1]
    q=sum(x['q'] for x in w)
    if q<=0: return False,None,None
    vwap=sum(((x['h']+x['l']+x['c'])/3)*x['q'] for x in w)/q
    x=w[-1]; p=w[-2]
    dev=x['c']/vwap-1
    buy=x['tb']/x['vol'] if x['vol']>0 else 0
    pbuy=p['tb']/p['vol'] if p['vol']>0 else 0
    t32=(dev<=-0.004 and x['c']>x['o'] and buy>=0.50 and buy>pbuy)
    peak=max(r['h'] for r in rows[i-719:i+1])
    gate=x['c']<=peak*0.96
    return t32 and gate, vwap, {'dev':dev,'buy':buy,'peak':peak}

def replay(rows):
    pending=None; pos=None; fills_day=defaultdict(int); trades=[]; signals=0; expired=0
    for i,r in enumerate(rows):
        # Existing position: pessimistic intrabar order, STOP before TP.
        if pos is not None:
            stop=price_for_net(pos['entry'], STOP_NET)
            tp=price_for_net(pos['entry'], TP_NET)
            held=i-pos['fill_i']
            reason=None; exitp=None
            if r['l']<=stop:
                base=min(stop,r['o'])
                exitp=base*(1-SLIP); reason='STOP'
            elif r['h']>=tp:
                exitp=tp; reason='TP'
            elif held>=MAX_HOLD:
                exitp=r['c']*(1-SLIP); reason='TIME'
            if reason:
                nr=net_ret(pos['entry'],exitp)
                trades.append({**pos,'exit_i':i,'exit_time':r['dt'].isoformat(),'exit':exitp,'net':nr,'reason':reason})
                pos=None
        # Pending limit orders fill only after signal minute, for next 2 candles.
        if pos is None and pending is not None:
            if i>pending['signal_i'] and i<=pending['signal_i']+TTL and r['l']<=pending['limit']:
                entry=min(pending['limit'],r['o'])
                day=r['dt'].date().isoformat()
                fills_day[day]+=1
                pos={'signal_i':pending['signal_i'],'signal_time':pending['signal_time'],'fill_i':i,'fill_time':r['dt'].isoformat(),'entry':entry,'signal_close':pending['signal_close']}
                pending=None
                # Conservative same-candle handling: STOP has priority; TP is not credited on fill candle.
                stop=price_for_net(entry, STOP_NET)
                if r['l']<=stop:
                    exitp=min(stop,r['o'])*(1-SLIP); nr=net_ret(entry,exitp)
                    trades.append({**pos,'exit_i':i,'exit_time':r['dt'].isoformat(),'exit':exitp,'net':nr,'reason':'STOP'})
                    pos=None
            elif i>pending['signal_i']+TTL:
                expired+=1; pending=None
        # New signal only at candle close and only when flat/no pending.
        if pos is None and pending is None:
            day=r['dt'].date().isoformat()
            if fills_day[day] < MAX_PER_DAY:
                ok,vwap,meta=signal(rows,i)
                if ok:
                    signals+=1
                    pending={'signal_i':i,'signal_time':r['dt'].isoformat(),'signal_close':r['c'],'limit':r['c']*(1-LIMIT_DISCOUNT),'vwap':vwap,'meta':meta}
    # mark any still-open position at final close, adverse slippage, for complete-month accounting
    if pos is not None:
        r=rows[-1]; exitp=r['c']*(1-SLIP); nr=net_ret(pos['entry'],exitp)
        trades.append({**pos,'exit_i':len(rows)-1,'exit_time':r['dt'].isoformat(),'exit':exitp,'net':nr,'reason':'EOM'})
    return signals, expired, trades

def stats(trades):
    nets=[t['net'] for t in trades]
    wins=[x for x in nets if x>0]; losses=[x for x in nets if x<=0]
    pf=(sum(wins)/abs(sum(losses))) if losses and sum(losses)!=0 else (float('inf') if wins else 0)
    comp=math.prod(1+x for x in nets)-1 if nets else 0
    eq=1.0; peak=1.0; mdd=0.0
    for x in nets:
        eq*=1+x; peak=max(peak,eq); mdd=min(mdd,eq/peak-1)
    reasons=defaultdict(int)
    for t in trades: reasons[t['reason']]+=1
    return {
        'trades':len(trades),'wins':len(wins),'win_rate_pct':100*len(wins)/len(trades) if trades else 0,
        'avg_net_pct':100*sum(nets)/len(nets) if nets else 0,'profit_factor':pf,
        'compound_pct':100*comp,'max_drawdown_pct':100*mdd,'sum_net_pct':100*sum(nets),
        'exits':dict(reasons)
    }

def main():
    rows=load(); signals,expired,trades=replay(rows); s=stats(trades)
    result={
        'test':'Frozen X AUTO X ECONOMY cross-market OOS replay','symbol':SYMBOL,'month':MONTH,'source':URL,
        'candles':len(rows),'period_utc':[rows[0]['dt'].isoformat(),rows[-1]['dt'].isoformat()],
        'parameters':{'fee_each_side_pct':FEE*100,'t32_deviation_pct':-0.4,'buy_share_min_pct':50,'below_12h_peak_pct':4,'limit_discount_pct':LIMIT_DISCOUNT*100,'ttl_min':TTL,'tp_net_pct':TP_NET*100,'stop_net_pct':STOP_NET*100,'max_hold_min':MAX_HOLD,'max_fills_per_utc_day':MAX_PER_DAY,'adverse_stop_time_slippage_pct':SLIP*100,'peak_basis':'12h candle HIGH'},
        'signals':signals,'expired_limits':expired,'stats':s,
        'trades':trades
    }
    print(json.dumps(result,ensure_ascii=False,indent=2))
    with open('sol_x_oos_result.json','w',encoding='utf-8') as f: json.dump(result,f,ensure_ascii=False,indent=2)
    print('\nSUMMARY')
    for k,v in s.items(): print(f'{k}: {v}')

if __name__=='__main__': main()
