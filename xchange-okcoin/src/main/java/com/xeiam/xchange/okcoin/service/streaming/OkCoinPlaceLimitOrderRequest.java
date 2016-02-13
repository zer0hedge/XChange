package com.xeiam.xchange.okcoin.service.streaming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.okcoin.OkCoinDigest;
import com.xeiam.xchange.okcoin.OkCoinStreamingUtils;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinTradeResult;
import com.xeiam.xchange.service.streaming.ExchangeEventType;

public class OkCoinPlaceLimitOrderRequest extends OkCoinWebSocketAPIRequest implements Future<String> {
  
  private LimitOrder limitOrder;
  
  public static final Long DUMMY_ID = -1L;

  OkCoinPlaceLimitOrderRequest(LimitOrder limitOrder, ChannelProvider channelProvider, String apikey,
      OkCoinDigest signatureCreator) {
    super(channelProvider);
    this.limitOrder = limitOrder;
    params = new HashMap<String, String>();
    params.put("api_key", apikey);
    params.put("symbol", limitOrder.getCurrencyPair().toString().replace("/", "_").toLowerCase());
    params.put("type", limitOrder.getType() == OrderType.ASK ? "sell" : "buy");
    params.put("price", limitOrder.getLimitPrice().toPlainString());
    params.put("amount", limitOrder.getTradableAmount().toPlainString());
    String sign = signatureCreator.digestNameValueParamMap(new ArrayList<>(params.entrySet()));
    params.put("sign", sign);
  }

  @Override
  String getChannel() {
    return channelProvider.getPlaceLimitOrder();
  }

  public String get() throws InterruptedException, ExecutionException {
    
    return checkOrderId((OkCoinTradeResult) super.get());
  }

  public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
    
    return checkOrderId((OkCoinTradeResult) super.get(timeout, unit));
  }

  public LimitOrder getOrder() {
    return limitOrder;
  }
  
  @Override
  RequestIdentifier getIdentifier() {
    return new RequestIdentifier(DUMMY_ID, ExchangeEventType.ORDER_ADDED);
  }


  String checkOrderId(OkCoinTradeResult result) {
    
    if (!result.isResult())
      throw new ExchangeException(OkCoinStreamingUtils.getErrorMessage(result.getErrorCode()));
    return Long.toString(result.getOrderId());
  }

}
