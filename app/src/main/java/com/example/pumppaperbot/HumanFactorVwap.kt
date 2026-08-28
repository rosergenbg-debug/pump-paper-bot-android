package com.example.pumppaperbot

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max

data class HumanFactorTrade(val time:Long,val action:String,val price:Double,val amount:Double,val fee:Double,val pnlEur:Double,val reason:String){
    fun json()=JSONObject().put("time",time).put("action",action).put("price",price).put("amount",amount).put("fee",fee).put("pnl",pnlEur).put("reason",reason)
    companion object{fun from(j:JSONObject)=HumanFactorTrade(j.optLong("time"),j.optString("action"),j.optDouble("price"),j.optDouble("amount"),j.optDouble("fee"),j.optDouble("pnl"),j.optString("reason"))}
}
data class HumanFactorState(
    val cash:Double=1000.0,val coins:Double=0.0,val entryPrice:Double=0.0,val entryAt:Long=0,
    val targetVwap:Double=0.0,val readiness:Int=0,val pending:Boolean=false,val candidateId:Long=0,
    val reason:String="Ожидаем минутные данные",val lastAlertBand:Int=0,val updatedAt:Long=0,
    val trades:List<HumanFactorTrade> = emptyList()
){val inPosition get()=coins>0.0;fun value(price:Double)=cash+coins*max(price,0.0)}

internal object HumanFactorVwapPolicy {
    const val READY=98
    fun evaluate(c:List<PumpCandle>):Triple<Int,Double,String>{
        if(c.size<61)return Triple(0,0.0,"Нужно не менее 60 закрытых минут")
        val rows=c.dropLast(1).takeLast(60);val x=rows.last();val prev=rows[rows.lastIndex-1]
        val q=rows.sumOf{it.quoteVolume};if(q<=0)return Triple(0,0.0,"Нет quote volume")
        val vwap=rows.sumOf{((it.high+it.low+it.close)/3.0)*it.quoteVolume}/q
        val deviation=(x.close/vwap-1.0)*100.0
        val buy=if(x.volume>0)x.takerBuyVolume/x.volume else 0.0
        val prevBuy=if(prev.volume>0)prev.takerBuyVolume/prev.volume else 0.0
        val distance=(((-deviation)/0.40)*55.0).toInt().coerceIn(0,55)
        val green=if(x.close>x.open)15 else 0
        val share=((buy-.40)/.10*20).toInt().coerceIn(0,20)
        val repair=if(buy>prevBuy)10 else 0
        val score=(distance+green+share+repair).coerceIn(0,100)
        val exact=deviation<=-.40&&x.close>x.open&&buy>=.50&&buy>prevBuy
        val final=if(exact)100 else score.coerceAtMost(99)
        return Triple(final,vwap,String.format(Locale.GERMANY,"VWAP €%.8f • отклонение %+.2f%% • BUY %.0f%%",vwap,deviation,buy*100))
    }
}

object HumanFactorStore {
    private const val PREFS="human_factor_vwap_v630";private const val KEY="state";private const val FEE=.0025
    fun state(c:Context):HumanFactorState=runCatching{val j=JSONObject(c.getSharedPreferences(PREFS,0).getString(KEY,"{}")!!);HumanFactorState(j.optDouble("cash",1000.0),j.optDouble("coins"),j.optDouble("entryPrice"),j.optLong("entryAt"),j.optDouble("targetVwap"),j.optInt("readiness"),j.optBoolean("pending"),j.optLong("candidateId"),j.optString("reason","Ожидание"),j.optInt("lastAlertBand"),j.optLong("updatedAt"),(j.optJSONArray("trades")?:JSONArray()).let{a->(0 until a.length()).map{HumanFactorTrade.from(a.getJSONObject(it))}})}.getOrDefault(HumanFactorState())
    private fun save(c:Context,s:HumanFactorState){val j=JSONObject().put("cash",s.cash).put("coins",s.coins).put("entryPrice",s.entryPrice).put("entryAt",s.entryAt).put("targetVwap",s.targetVwap).put("readiness",s.readiness).put("pending",s.pending).put("candidateId",s.candidateId).put("reason",s.reason).put("lastAlertBand",s.lastAlertBand).put("updatedAt",s.updatedAt).put("trades",JSONArray(s.trades.takeLast(300).map{it.json()}));c.getSharedPreferences(PREFS,0).edit().putString(KEY,j.toString()).commit()}
    @Synchronized fun sync(c:Context,now:Long=System.currentTimeMillis()):HumanFactorState{
        var s=state(c);val market=BitpandaFusionStore.state(c);val price=market.bid.takeIf{market.fresh(now)}?:PaperExecutionPolicy.displayPrice(PumpBotEngine.snapshot(c),now)
        if(s.inPosition){val net=((price*(1-FEE))/(s.entryPrice*(1+FEE))-1)*100;val exit=price>=s.targetVwap||net<=-.80||now-s.entryAt>=90*60_000L;if(exit){val gross=s.coins*price;val fee=gross*FEE;val pnl=gross-fee-(s.coins*s.entryPrice*(1+FEE));s=s.copy(cash=gross-fee,coins=0.0,entryPrice=0.0,entryAt=0,targetVwap=0.0,readiness=0,pending=false,reason="Виртуальный выход: ${if(price>=s.targetVwap)"VWAP" else if(net<=-.80)"STOP" else "90 МИН"}",trades=s.trades+HumanFactorTrade(now,"SELL",price,s.coins,fee,pnl,"VWAP/STOP/TIME"));save(c,s);PumpAlert.showHumanFactor(c,false,s.reason)};return s}
        val priorBand=s.lastAlertBand;val (score,vwap,reason)=HumanFactorVwapPolicy.evaluate(ChartSpeedStore.candles(c,ChartInterval.ONE_MINUTE));val rejected=s.candidateId==-1L;val pending=score>=HumanFactorVwapPolicy.READY&&!rejected;val id=when{score<90->0L;pending&&(!s.pending||s.candidateId==0L)->now;else->s.candidateId};val band=when{rejected->0;score>=98->100;score>=90->90;else->0};s=s.copy(readiness=score,pending=pending,candidateId=id,reason=if(rejected)"Вход отклонён; ждём распада текущей ситуации" else reason,lastAlertBand=band,updatedAt=now,targetVwap=vwap);save(c,s);if(band>priorBand)PumpAlert.showHumanFactor(c,true,"Готовность $score/100. $reason");return s
    }
    @Synchronized fun approve(c:Context,now:Long=System.currentTimeMillis()):Boolean{val s=state(c);if(!s.pending||s.readiness<HumanFactorVwapPolicy.READY||s.inPosition)return false;val m=BitpandaFusionStore.state(c);val ask=m.ask.takeIf{m.fresh(now)}?:return false;val fee=s.cash*FEE;val coins=(s.cash-fee)/ask;save(c,s.copy(cash=0.0,coins=coins,entryPrice=ask,entryAt=now,pending=false,readiness=0,reason="ПОКУПКА ПОДТВЕРЖДЕНА ЧЕЛОВЕКОМ",trades=s.trades+HumanFactorTrade(now,"BUY",ask,coins,fee,0.0,"HUMAN_APPROVED")));return true}
    @Synchronized fun reject(c:Context){val s=state(c);save(c,s.copy(pending=false,candidateId=-1,reason="Вход отклонён человеком; ждём нового рыночного setup"))}
}

object Vwap3265AutoStore {
    private const val PREFS="vwap_3265_auto_v630";private const val KEY="state";private const val FEE=.0025
    fun state(c:Context):HumanFactorState=runCatching{val j=JSONObject(c.getSharedPreferences(PREFS,0).getString(KEY,"{}")!!);HumanFactorState(j.optDouble("cash",1000.0),j.optDouble("coins"),j.optDouble("entryPrice"),j.optLong("entryAt"),j.optDouble("targetVwap"),j.optInt("readiness"),false,0,j.optString("reason","Ожидание"),0,j.optLong("updatedAt"),(j.optJSONArray("trades")?:JSONArray()).let{a->(0 until a.length()).map{HumanFactorTrade.from(a.getJSONObject(it))}})}.getOrDefault(HumanFactorState())
    private fun save(c:Context,s:HumanFactorState){val j=JSONObject().put("cash",s.cash).put("coins",s.coins).put("entryPrice",s.entryPrice).put("entryAt",s.entryAt).put("targetVwap",s.targetVwap).put("readiness",s.readiness).put("reason",s.reason).put("updatedAt",s.updatedAt).put("trades",JSONArray(s.trades.takeLast(300).map{it.json()}));c.getSharedPreferences(PREFS,0).edit().putString(KEY,j.toString()).commit()}
    @Synchronized fun sync(c:Context,now:Long=System.currentTimeMillis()):HumanFactorState{
        var s=state(c);val market=BitpandaFusionStore.state(c);val bid=market.bid.takeIf{market.fresh(now)}?:PaperExecutionPolicy.displayPrice(PumpBotEngine.snapshot(c),now)
        if(s.inPosition){val n=((bid*(1-FEE))/(s.entryPrice*(1+FEE))-1)*100;if(bid>=s.targetVwap||n<=-.80||now-s.entryAt>=90*60_000L){val gross=s.coins*bid;val fee=gross*FEE;val pnl=gross-fee-(s.coins*s.entryPrice*(1+FEE));s=s.copy(cash=gross-fee,coins=0.0,entryPrice=0.0,entryAt=0,targetVwap=0.0,readiness=0,reason="AUTO EXIT",trades=s.trades+HumanFactorTrade(now,"SELL",bid,s.coins,fee,pnl,"AUTO_VWAP_STOP_TIME"));save(c,s)};return s}
        val(score,vwap,reason)=HumanFactorVwapPolicy.evaluate(ChartSpeedStore.candles(c,ChartInterval.ONE_MINUTE));if(score>=100){val ask=market.ask.takeIf{market.fresh(now)};if(ask!=null){val fee=s.cash*FEE;val coins=(s.cash-fee)/ask;s=s.copy(cash=0.0,coins=coins,entryPrice=ask,entryAt=now,targetVwap=vwap,readiness=0,reason="AUTO BUY • $reason",trades=s.trades+HumanFactorTrade(now,"BUY",ask,coins,fee,0.0,"AUTO_VWAP_3265"));save(c,s);return s}}
        s=s.copy(readiness=score,targetVwap=vwap,reason=reason,updatedAt=now);save(c,s);return s
    }
}
