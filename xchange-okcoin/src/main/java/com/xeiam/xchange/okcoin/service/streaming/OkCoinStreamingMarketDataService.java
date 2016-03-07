package com.xeiam.xchange.okcoin.service.streaming;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.service.streaming.ExchangeStreamingConfiguration;
import com.xeiam.xchange.service.streaming.StreamingMarketDataService;

public class OkCoinStreamingMarketDataService extends OkCoinBaseStreamingService implements StreamingMarketDataService {

  public OkCoinStreamingMarketDataService(Exchange exchange,
      ExchangeStreamingConfiguration exchangeStreamingConfiguration) {
    super(exchange, exchangeStreamingConfiguration, "MDS");
  }

  public void addTickerChannel(CurrencyPair currencyPair) {
    getSocketBase().addChannel(channelProvider.getTicker(currencyPair));
  }

  public void addDepthChannel(CurrencyPair currencyPair) {
    getSocketBase().addChannel(channelProvider.getDepth20(currencyPair));
  }

  public void addTradesChannel(CurrencyPair currencyPair) {
    getSocketBase().addChannel(channelProvider.getTrades(currencyPair));
  }

}
