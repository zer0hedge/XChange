package com.xeiam.xchange.okcoin.dto.trade;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OkCoinOrdersResult extends OkCoinErrorResult {

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
