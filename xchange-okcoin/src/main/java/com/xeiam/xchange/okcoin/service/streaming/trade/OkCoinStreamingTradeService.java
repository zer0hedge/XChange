package com.xeiam.xchange.okcoin.service.streaming.trade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;
import com.xeiam.xchange.okcoin.OkCoinAdapters;
import com.xeiam.xchange.okcoin.OkCoinDigest;
import com.xeiam.xchange.okcoin.OkCoinStreamingUtils;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrdersResult;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinTradeResult;
import com.xeiam.xchange.okcoin.service.streaming.OkCoinBaseStreamingService;
import com.xeiam.xchange.service.streaming.ExchangeStreamingConfiguration;
import com.xeiam.xchange.service.streaming.trade.StreamingTradeService;

public class OkCoinStreamingTradeService extends OkCoinBaseStreamingService implements StreamingTradeService {

  private String apikey;
  private OkCoinDigest signatureCreator;

  public OkCoinStreamingTradeService(Exchange exchange, ExchangeStreamingConfiguration exchangeStreamingConfiguration) {
    super(exchange, exchangeStreamingConfiguration);
    apikey = exchange.getExchangeSpecification().getApiKey();
    signatureCreator = new OkCoinDigest(apikey, exchange.getExchangeSpecification().getSecretKey());
  }

  @Override
  public synchronized String placeLimitOrder(LimitOrder limitOrder)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    Map<String, String> params = new HashMap<>();
    params.put("api_key", apikey);
    params.put("symbol", limitOrder.getCurrencyPair().toString().replace("/", "_").toLowerCase());
    params.put("type", limitOrder.getType() == OrderType.ASK ? "sell" : "buy");
    params.put("price", limitOrder.getLimitPrice().toPlainString());
    params.put("amount", limitOrder.getTradableAmount().toPlainString());
    String sign = signatureCreator.digestNameValueParamMap(new ArrayList<>(params.entrySet()));
    params.put("sign", sign);

    getSocketBase().addChannel(channelProvider.getPlaceLimitOrder(), params);
    ;
    try {
      OkCoinTradeResult result = (OkCoinTradeResult) getNextEvent().getPayload();
      if (result.isResult()) {
        knownOrders.put(String.valueOf(result.getOrderId()), limitOrder);
        return String.valueOf(result.getOrderId());
      } else
        throw new ExchangeException(OkCoinStreamingUtils.getErrorMessage(result.getErrorCode()));
    } catch (InterruptedException e) {
      return null;
    }
  }

  @Override
  public synchronized void cancelOrder(String orderId)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    Map<String, String> params = new HashMap<>();
    params.put("api_key", apikey);
    params.put("symbol", knownOrders.get(orderId).getCurrencyPair().toString().replace("/", "_").toLowerCase());
    params.put("order_id", orderId);
    String sign = signatureCreator.digestNameValueParamMap(new ArrayList<>(params.entrySet()));
    params.put("sign", sign);

    getSocketBase().addChannel(channelProvider.getCancelOrder(), params);

    try {
      OkCoinTradeResult result = (OkCoinTradeResult) getNextEvent().getPayload();
      if (!result.isResult())
        throw new ExchangeException(OkCoinStreamingUtils.getErrorMessage(result.getErrorCode()));
    } catch (InterruptedException e) {
      return;
    }
  }

  @Override
  public synchronized LimitOrder getOrder(String orderId)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    Map<String, String> params = new HashMap<>();
    params.put("api_key", apikey);
    params.put("symbol", knownOrders.get(orderId).getCurrencyPair().toString().replace("/", "_").toLowerCase());
    params.put("order_id", orderId);
    String sign = signatureCreator.digestNameValueParamMap(new ArrayList<>(params.entrySet()));
    params.put("sign", sign);
    getSocketBase().addChannel(channelProvider.getOrderInfo(), params);

    try {
      OkCoinOrdersResult result = (OkCoinOrdersResult) getNextEvent().getPayload();
      if (result.isResult())
        return OkCoinAdapters.adaptOrder(result.getOrders()[0]);
        throw new ExchangeException(OkCoinStreamingUtils.getErrorMessage(result.getErrorCode()));
    } catch (InterruptedException e) {
      return null;
    }
  }

  private Map<String, LimitOrder> knownOrders = new HashMap<>();

}
