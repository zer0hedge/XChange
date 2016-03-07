package com.xeiam.xchange.okcoin.service.streaming;

import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.okcoin.FuturesContract;

class FuturesChannelProvider implements ChannelProvider {
  private final String contractName;

  FuturesChannelProvider(FuturesContract contract) {
    contractName = contract.getName();
  }

  private static String pairToString(CurrencyPair currencyPair) {
    return currencyPair.base.getCurrencyCode().toLowerCase() + currencyPair.counter.getCurrencyCode().toLowerCase();
  }

  @Override
  public String getTicker(CurrencyPair currencyPair) {
    return "ok_sub_future" + pairToString(currencyPair) + "_ticker_" + contractName;
  }

  @Override
  public String getDepth20(CurrencyPair currencyPair) {
    return "ok_sub_future" + pairToString(currencyPair) + "_depth_" + contractName+"_20";
  }

  @Override
  public String getTrades(CurrencyPair currencyPair) {
    return "ok_sub_future" + pairToString(currencyPair) + "_trade_" + contractName;
  }
  
  @Override
  public String getOrderInfo() {
	  return "ok_futureusd_order_info";
  }
  
  @Override
  public String getPlaceLimitOrder() {
    return "ok_futureusd_trade";
  } 
  
  @Override
  public String getCancelOrder() {
    return "ok_futuresusd_cancel_order";
  }
}