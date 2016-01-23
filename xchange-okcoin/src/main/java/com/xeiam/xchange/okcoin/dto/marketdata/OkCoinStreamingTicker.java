package com.xeiam.xchange.okcoin.dto.marketdata;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OkCoinStreamingTicker extends OkCoinTicker {

  private final long timestamp;
  private final BigDecimal unitAmount;
  private final String contractId;
  private final int holdAmount;

  public OkCoinStreamingTicker(@JsonProperty("high") final BigDecimal high, @JsonProperty("low") final BigDecimal low,
      @JsonProperty("buy") final BigDecimal buy, @JsonProperty("sell") final BigDecimal sell, @JsonProperty("last") final BigDecimal last,
      @JsonProperty("vol") final BigDecimal vol, @JsonProperty("timestamp") long timestamp, @JsonProperty("hold_amount") int holdAmount,
      @JsonProperty("unitAmount") final BigDecimal unitAmount, @JsonProperty("contractId") String contractId) {
	super(high, low, buy, sell, last, vol);
    this.holdAmount = holdAmount;
    this.timestamp = timestamp;
    this.unitAmount = unitAmount;
    this.contractId = contractId;
  }

  public BigDecimal getUnitAmount() {

    return unitAmount;
  }

  public String getContractId() {

    return contractId;
  }

  public int getHoldAmount() {

    return holdAmount;
  }

  /**
   * @return the timestamp
   */
  public long getTimestamp() {
    return timestamp;
  }

}
