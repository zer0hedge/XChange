package com.xeiam.xchange.okcoin.service.streaming;

import com.xeiam.xchange.currency.CurrencyPair;

/** Returns market data channel name for given connection method and currency pair */
public interface ChannelProvider {
  String getTicker(CurrencyPair currencyPair);

  String getDepth(CurrencyPair currencyPair);

  String getTrades(CurrencyPair currencyPair);
  
  String getOrderInfo();
  
  String getPlaceLimitOrder();
  
  String getCancelOrder();
  
}