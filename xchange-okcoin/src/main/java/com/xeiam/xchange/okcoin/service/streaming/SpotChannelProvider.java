package com.xeiam.xchange.okcoin.service.streaming;

import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.currency.CurrencyPair;

class SpotChannelProvider implements ChannelProvider {

  SpotChannelProvider(ExchangeSpecification exchangeSpecification) {
    this.exchangeSpecification = exchangeSpecification;
  }

  private static String pairToString(CurrencyPair currencyPair) {
    return currencyPair.counter.getCurrencyCode().toLowerCase() +"_"+ currencyPair.base.getCurrencyCode().toLowerCase();
  }

  @Override
  public String getTicker(CurrencyPair currencyPair) {
    return "ok_sub_spot" + pairToString(currencyPair) + "_ticker";
  }

  @Override
  public String getDepth20(CurrencyPair currencyPair) {
    return "ok_sub_spot" + pairToString(currencyPair) + "_depth_20";
  }

  @Override
  public String getTrades(CurrencyPair currencyPair) {
    return "ok_sub_spot" + pairToString(currencyPair) + "_trades";
  }

  @Override
  public String getOrderInfo() {
    String currencyString = exchangeSpecification.getExchangeSpecificParametersItem("Use_Intl").equals(false) ? "cny"
        : "usd";
    return "ok_spot" + currencyString + "_order_info";
  }

  @Override
  public String getPlaceLimitOrder() {
    String currencyString = exchangeSpecification.getExchangeSpecificParametersItem("Use_Intl").equals(false) ? "cny"
        : "usd";
    return "ok_spot" + currencyString + "_trade";
  }
  
  @Override
  public String getCancelOrder() {
    String currencyString = exchangeSpecification.getExchangeSpecificParametersItem("Use_Intl").equals(false) ? "cny"
        : "usd";
    return "ok_spot" + currencyString + "_cancel_order";
  }

  private ExchangeSpecification exchangeSpecification;

}