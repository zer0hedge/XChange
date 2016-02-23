package com.xeiam.xchange.service.streaming;

import com.xeiam.xchange.currency.CurrencyPair;

public interface StreamingMarketDataService extends StreamingExchangeService {

  public void addTickerChannel(CurrencyPair currencyPair);
  
  public void addDepthChannel(CurrencyPair currencyPair);
  
  public void addTradesChannel(CurrencyPair currencyPair);
  
}
