package com.xeiam.xchange.okcoin.dto.trade;

public class OkCoinGetOrderInfoError extends OkCoinTradeResult {

  public OkCoinGetOrderInfoError(final boolean result, final int errorCode,
      final long orderId) {
    super(result, errorCode, orderId);
  }
}
