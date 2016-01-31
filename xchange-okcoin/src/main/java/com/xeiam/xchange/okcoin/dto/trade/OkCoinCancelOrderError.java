package com.xeiam.xchange.okcoin.dto.trade;

public class OkCoinCancelOrderError extends OkCoinTradeResult {

  public OkCoinCancelOrderError(final boolean result, final int errorCode,
      final long orderId) {
    super(result, errorCode, orderId);
  }
}
