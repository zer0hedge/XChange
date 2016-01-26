package com.xeiam.xchange.okcoin.service.streaming;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.xeiam.xchange.service.streaming.ExchangeEvent;
import com.xeiam.xchange.service.streaming.ExchangeEventType;
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

    while (true) {
      getSocketBase().addOneTimeChannel(channelProvider.getPlaceLimitOrder(), params);

      try {
        ExchangeEvent event = getNextEvent();
        if (event.getEventType() != ExchangeEventType.DISCONNECT) {
          OkCoinTradeResult result = (OkCoinTradeResult) event.getPayload();
          if (result.isResult()) {
            knownOrders.put(String.valueOf(result.getOrderId()), limitOrder);
            return String.valueOf(result.getOrderId());
          } else
            throw new ExchangeException(OkCoinStreamingUtils.getErrorMessage(result.getErrorCode()));
        } else {
          log.warn("IO error, retrying");
          continue;
        }
      } catch (InterruptedException e) {
        return null;
      }
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

    while (true) {
      getSocketBase().addOneTimeChannel(channelProvider.getCancelOrder(), params);
      try {
        ExchangeEvent event = getNextEvent();
        if (event.getEventType() != ExchangeEventType.DISCONNECT) {
          OkCoinTradeResult result = (OkCoinTradeResult) event.getPayload();
          if (!result.isResult())
            throw new ExchangeException(OkCoinStreamingUtils.getErrorMessage(result.getErrorCode()));
          return;
        } else {
          log.warn("IO error, retrying");
          continue;
        }
      } catch (InterruptedException e) {
        return;
      }
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

    while (true) {
      getSocketBase().addOneTimeChannel(channelProvider.getOrderInfo(), params);
      try {
        ExchangeEvent event = getNextEvent();
        if (event.getEventType() != ExchangeEventType.DISCONNECT) {
          OkCoinOrdersResult result = (OkCoinOrdersResult) event.getPayload();
          if (result.isResult())
            return OkCoinAdapters.adaptOrder(result.getOrders()[0]);
          throw new ExchangeException(OkCoinStreamingUtils.getErrorMessage(result.getErrorCode()));
        } else {
          log.warn("IO error, retrying");
          continue;
        }
      } catch (InterruptedException e) {
        return null;
      }
    }
  }

  private Map<String, LimitOrder> knownOrders = new HashMap<>();
  private Logger log = LoggerFactory.getLogger(this.getClass());
  
}
