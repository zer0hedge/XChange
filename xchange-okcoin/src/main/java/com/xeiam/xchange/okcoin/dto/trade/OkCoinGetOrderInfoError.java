package com.xeiam.xchange.okcoin.dto.trade;

public class OkCoinGetOrderInfoError extends OkCoinTradeResult {

  @Override
	public String toString() {
		return "OkCoinGetOrderInfoError [" + (super.toString() != null ? "toString()=" + super.toString() : "") + "]";
	}

public OkCoinGetOrderInfoError(final boolean result, final int errorCode,
      final long orderId) {
    super(result, errorCode, orderId);
  }
}
