package com.xeiam.xchange.okcoin.dto.trade;

public class OkCoinPlaceOrderError extends OkCoinTradeResult {

  public OkCoinPlaceOrderError(final boolean result, final int errorCode,
      final long orderId) {

    super(result, errorCode, orderId);
  }

}
