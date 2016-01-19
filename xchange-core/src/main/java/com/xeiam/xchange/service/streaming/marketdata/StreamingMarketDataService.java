package com.xeiam.xchange.service.streaming.marketdata;

import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.service.streaming.StreamingExchangeService;

public interface StreamingMarketDataService extends StreamingExchangeService {

  public void addTickerChannel(CurrencyPair currencyPair);
  
  public void addDepthChannel(CurrencyPair currencyPair);
  
  public void addTradesChannel(CurrencyPair currencyPair);
  
}
