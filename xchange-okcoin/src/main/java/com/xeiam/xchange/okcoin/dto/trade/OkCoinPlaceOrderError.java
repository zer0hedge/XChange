package com.xeiam.xchange.okcoin.dto.trade;

public class OkCoinPlaceOrderError extends OkCoinTradeResult {

  @Override
	public String toString() {
		return "OkCoinPlaceOrderError [" + (super.toString() != null ? "toString()=" + super.toString() : "") + "]";
	}

public OkCoinPlaceOrderError(final boolean result, final int errorCode,
      final long orderId) {

    super(result, errorCode, orderId);
  }

}
