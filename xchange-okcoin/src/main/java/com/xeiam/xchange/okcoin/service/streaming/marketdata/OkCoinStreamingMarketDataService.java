package com.xeiam.xchange.okcoin.service.streaming.marketdata;

import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.okcoin.service.streaming.OkCoinStreamingExchangeService;
import com.xeiam.xchange.service.streaming.ExchangeStreamingConfiguration;
import com.xeiam.xchange.service.streaming.marketdata.StreamingMarketDataService;

public class OkCoinStreamingMarketDataService extends OkCoinStreamingExchangeService
    implements StreamingMarketDataService {

  public OkCoinStreamingMarketDataService(ExchangeSpecification exchangeSpecification,
      ExchangeStreamingConfiguration exchangeStreamingConfiguration) {
    super(exchangeSpecification, exchangeStreamingConfiguration);
  }

  public void addTickerChannel(CurrencyPair currencyPair) {
    socketBase.addChannel(channelProvider.getTicker(currencyPair));
  }

  public void addDepthChannel(CurrencyPair currencyPair) {
    socketBase.addChannel(channelProvider.getDepth(currencyPair));
  }

  public void addTradesChannel(CurrencyPair currencyPair) {
    socketBase.addChannel(channelProvider.getTrades(currencyPair));
  }

}
