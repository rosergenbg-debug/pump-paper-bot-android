# PUMP RSI35 Signal Android

Separate Android app for RSI35 signal monitoring and paper-style backtests.

## Main mode

- Main live asset: PUMP/USDT.
- Data source: public Binance 30-minute candles, no API key.
- Strategy: RSI14 on 30m candles.
- BUY signal: RSI <= 35 and price is above EMA200.
- SELL signal: RSI >= 62, or price falls below EMA200, or stop/trailing protection is hit.
- Fee model in backtest: 0.15% per buy and per sell.
- Slippage model in backtest: 0.05%.
- The app remembers whether you are waiting for BUY or waiting for SELL.
- If you confirm a manual BUY, it stores the entry price and starts waiting for SELL.
- If you confirm a manual SELL, it clears the entry price and starts waiting for BUY.

## Alerts

- START MONITOR launches a foreground service with a permanent Android notification.
- While running, the service checks PUMP about every 2 minutes.
- When the active mode matches a new BUY or SELL signal, the phone shows a high-priority notification with sound and vibration.
- Android may still limit background work depending on battery settings, but a foreground service is much more reliable than a normal background worker.

## Backtest

- Backtest screen lets you choose a start date and one of 10 coins.
- Coins: PUMP, BTC, ETH, SOL, BNB, XRP, ADA, DOGE, TRX, LINK.
- Backtest uses the same RSI35 strategy on 30-minute candles.

This is not automatic real-money trading. It is a signal and testing app.
