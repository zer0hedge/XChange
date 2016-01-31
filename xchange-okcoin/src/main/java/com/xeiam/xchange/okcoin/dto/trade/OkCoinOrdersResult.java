package com.xeiam.xchange.okcoin.dto.trade;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OkCoinOrdersResult extends OkCoinErrorResult {

  @Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("OkCoinOrdersResult [");
		if (orders != null) {
			builder.append("orders=");
			builder.append(Arrays.toString(orders));
		}
		builder.append("]");
		return builder.toString();
	}

private final OkCoinOrder[] orders;

  public OkCoinOrdersResult(@JsonProperty("result") final boolean result, @JsonProperty("error_code") final int errorCode,
      @JsonProperty("orders") final OkCoinOrder[] orders) {

    super(result, errorCode);
    this.orders = orders;
  }

  public OkCoinOrder[] getOrders() {

    return orders;
  }
}
