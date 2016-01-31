package com.xeiam.xchange.okcoin.dto.trade;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OkCoinErrorResult {

  @Override
	public String toString() {
		return "OkCoinErrorResult [result=" + result + ", errorCode=" + errorCode + "]";
	}

private final boolean result;
  private final int errorCode;

  public OkCoinErrorResult(@JsonProperty("result") final boolean result, @JsonProperty("error_code") final int errorCode) {

    this.result = result;
    this.errorCode = errorCode;
  }

  public boolean isResult() {

    return result;
  }

  public int getErrorCode() {

    return errorCode;
  }

}
