package com.xeiam.xchange.okcoin.dto.trade;

public class OkCoinCancelOrderError extends OkCoinTradeResult {

  @Override
	public String toString() {
		return "OkCoinCancelOrderError [" + (super.toString() != null ? "toString()=" + super.toString() : "") + "]";
	}

public OkCoinCancelOrderError(final boolean result, final int errorCode,
      final long orderId) {
    super(result, errorCode, orderId);
  }
}
