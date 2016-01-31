package com.xeiam.xchange.okcoin.dto.trade;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OkCoinOrder {

  @Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("OkCoinOrder [orderId=");
		builder.append(orderId);
		builder.append(", status=");
		builder.append(status);
		builder.append(", ");
		if (symbol != null) {
			builder.append("symbol=");
			builder.append(symbol);
			builder.append(", ");
		}
		if (type != null) {
			builder.append("type=");
			builder.append(type);
			builder.append(", ");
		}
		if (amount != null) {
			builder.append("amount=");
			builder.append(amount);
			builder.append(", ");
		}
		if (dealAmount != null) {
			builder.append("dealAmount=");
			builder.append(dealAmount);
			builder.append(", ");
		}
		if (createDate != null) {
			builder.append("createDate=");
			builder.append(createDate);
			builder.append(", ");
		}
		if (price != null) {
			builder.append("price=");
			builder.append(price);
			builder.append(", ");
		}
		if (averagePrice != null) {
			builder.append("averagePrice=");
			builder.append(averagePrice);
		}
		builder.append("]");
		return builder.toString();
	}

public OkCoinOrder(@JsonProperty("order_id") final long orderId, @JsonProperty("status") final int status,
      @JsonProperty("symbol") final String symbol, @JsonProperty("type") final String type,
      @JsonProperty("price") final BigDecimal price, @JsonProperty("avg_price") final BigDecimal averagePrice,
      @JsonProperty("amount") final BigDecimal amount, @JsonProperty("deal_amount") final BigDecimal dealAmount,
      @JsonProperty("create_date") final Date createDate) {

    this.orderId = orderId;
    this.status = status;
    this.symbol = symbol;
    this.type = type;
    this.amount = amount;
    this.dealAmount = dealAmount;
    this.price = price;
    this.averagePrice = averagePrice;
    this.createDate = createDate;
  }

  private final long orderId;

  private final int status;

  private final String symbol;

  private final String type;

  private final BigDecimal amount;

  private final BigDecimal dealAmount;

  private final Date createDate;

  private final BigDecimal price;

  private final BigDecimal averagePrice;

  public long getOrderId() {

    return orderId;
  }

  public int getStatus() {

    return status;
  }

  public String getSymbol() {

    return symbol;
  }

  public String getType() {

    return type;
  }

  public BigDecimal getAmount() {

    return amount;
  }

  public BigDecimal getDealAmount() {

    return dealAmount;
  }

  public Date getCreateDate() {

    return createDate;
  }

  public BigDecimal getPrice() {

    return price;
  }

  public BigDecimal getAveragePrice() {

    return averagePrice;
  }
}
