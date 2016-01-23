package com.xeiam.xchange.okcoin.service.streaming;

import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.service.streaming.ExchangeStreamingConfiguration;

public class OkCoinStreamingConfiguration implements ExchangeStreamingConfiguration {

  private final CurrencyPair[] marketDataCurrencyPairs;

  public OkCoinStreamingConfiguration() {
    marketDataCurrencyPairs = new CurrencyPair[] { CurrencyPair.BTC_CNY };
  }

  public OkCoinStreamingConfiguration(CurrencyPair[] marketDataCurrencyPairs) {
    this.marketDataCurrencyPairs = marketDataCurrencyPairs;
  }

  @Override
  public int getMaxReconnectAttempts() {
    return 0;
  }

  @Override
  public int getReconnectWaitTimeInMs() {
    return 0;
  }

  @Override
  public int getTimeoutInMs() {
    return 0;
  }

  @Override
  public boolean isEncryptedChannel() {
    return false;
  }

  @Override
  public boolean keepAlive() {
    return false;
  }

  public CurrencyPair[] getMarketDataCurrencyPairs() {
    return marketDataCurrencyPairs;
  }
}
