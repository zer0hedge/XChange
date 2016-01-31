package com.xeiam.xchange.okcoin.dto.trade;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OkCoinTradeResult extends OkCoinErrorResult {

  @Override
	public String toString() {
		return "OkCoinTradeResult [orderId=" + orderId + ", "
				+ (super.toString() != null ? "toString()=" + super.toString() : "") + "]";
	}

private final long orderId;

  public OkCoinTradeResult(@JsonProperty("result") final boolean result, @JsonProperty("error_code") final int errorCode,
      @JsonProperty("order_id") final long orderId) {

    super(result, errorCode);
    this.orderId = orderId;
  }

  public long getOrderId() {

    return orderId;
  }
}
