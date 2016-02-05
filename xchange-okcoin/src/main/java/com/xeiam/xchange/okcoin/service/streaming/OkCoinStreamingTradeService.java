package com.xeiam.xchange.okcoin.service.streaming;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;
import com.xeiam.xchange.okcoin.OkCoinAdapters;
import com.xeiam.xchange.okcoin.OkCoinDigest;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinCancelOrderError;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinGetOrderInfoError;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrdersResult;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinPlaceOrderError;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinTradeResult;
import com.xeiam.xchange.service.streaming.ExchangeEvent;
import com.xeiam.xchange.service.streaming.ExchangeStreamingConfiguration;
import com.xeiam.xchange.service.streaming.trade.StreamingTradeService;

public class OkCoinStreamingTradeService extends OkCoinBaseStreamingService implements StreamingTradeService {

  private String apikey;
  private OkCoinDigest signatureCreator;
  private final BlockingQueue<OkCoinWebSocketAPIRequest> newRequestsQueue = new LinkedBlockingQueue<OkCoinWebSocketAPIRequest>();

  public OkCoinStreamingTradeService(Exchange exchange, ExchangeStreamingConfiguration exchangeStreamingConfiguration) {
    super(exchange, exchangeStreamingConfiguration);
    apikey = exchange.getExchangeSpecification().getApiKey();
    signatureCreator = new OkCoinDigest(apikey, exchange.getExchangeSpecification().getSecretKey());
  }

  @Override
  public synchronized String placeLimitOrder(LimitOrder limitOrder) {

    try {
      OkCoinPlaceLimitOrderRequest request = new OkCoinPlaceLimitOrderRequest(limitOrder, channelProvider, apikey,
          signatureCreator);
      newRequestsQueue.put(request);
      return request.get();

    } catch (InterruptedException e) {
      log.error("Unable to place order", e);
      return null;
    }

  }

  @Override
  public void cancelOrder(String orderId)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    Future<Boolean> res = cancelOrderNonBlocking(orderId, knownOrders.get(orderId).getCurrencyPair());
    try {
      res.get();
    } catch (InterruptedException | ExecutionException e) {
      log.error("Unable to cancel order", e);
    }
    return;

  }

  public Future<Boolean> cancelOrderNonBlocking(String orderId, CurrencyPair currencyPair) {
    try {
      OkCoinCancelOrderRequest request = new OkCoinCancelOrderRequest(orderId,
          currencyPair.toString().replace("/", "_").toLowerCase(), channelProvider, apikey, signatureCreator);
      newRequestsQueue.put(request);
      return request;

    } catch (InterruptedException e) {
      return null;
    }
  }

  @Override
  public LimitOrder getOrder(String orderId)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    Future<LimitOrder> res = getOrderNonBlocking(orderId, knownOrders.get(orderId).getCurrencyPair());
    try {
      return res.get();
    } catch (InterruptedException | ExecutionException e) {
      log.error("Unable to cancel order", e);
    }
    return null;

  }

  public Future<LimitOrder> getOrderNonBlocking(String orderId, CurrencyPair currencyPair) {
    try {
      OkCoinGetOrderInfoRequest request = new OkCoinGetOrderInfoRequest(orderId,
          currencyPair.toString().replace("/", "_").toLowerCase(), channelProvider, apikey, signatureCreator);
      newRequestsQueue.put(request);
      return request;

    } catch (InterruptedException e) {
      return null;
    }

  }

  private ConcurrentMap<String, LimitOrder> knownOrders = new ConcurrentHashMap<>();
  private ConcurrentMap<Long, OkCoinWebSocketAPIRequest> requests = new ConcurrentHashMap<>();
  private Logger log = LoggerFactory.getLogger(this.getClass());
  private ExecutorService executor;

  @Override
  public void connect() {
    executor = Executors.newFixedThreadPool(2);
    executor.execute(new Runnable() {

      @Override
      public void run() {
        Thread.currentThread().setName("Request dispatcher");
        while (!Thread.currentThread().isInterrupted()) {
          try {
            
            OkCoinWebSocketAPIRequest request = newRequestsQueue.take();
            requests.put(request.getId(), request);
            log.debug("Waiting for {}", request.getId());
            getSocketBase().addOneTimeChannel(request.getChannel(), request.getParams());
            
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    });

    executor.execute(new Runnable() {

      @Override
      public void run() {
        Thread.currentThread().setName("Response dispatcher");
        while (!Thread.currentThread().isInterrupted()) {
          try {
            ExchangeEvent event = getNextEvent();

            Object payload = event.getPayload();
            OkCoinWebSocketAPIRequest req;
            switch (event.getEventType()) {
            case ORDER_ADDED:
              log.debug("Processed addition of new order {}", ((OkCoinTradeResult) payload).getOrderId());
              req = requests.get(OkCoinWebSocketAPIRequest.DEFAULT_REQUEST_ID);
              if (req != null)
                req.set((OkCoinTradeResult) payload);
              else
                log.error("Unexpected {} event: {}", event.getEventType(), event);
              requests.remove(((OkCoinTradeResult) payload).getOrderId());
              break;
            case ORDER_CANCELED:
              log.debug("Processed cancellation for {}", ((OkCoinTradeResult) payload).getOrderId());
              req = requests.get(((OkCoinTradeResult) payload).getOrderId());
              if (req != null)
                req.set(true);
              else
                log.error("Unexpected {} event: {}", event.getEventType(), event);
              requests.remove(((OkCoinTradeResult) payload).getOrderId());
              break;
            case USER_ORDER:
              LimitOrder order = OkCoinAdapters.adaptOrder(((OkCoinOrdersResult) payload).getOrders()[0]);
              log.debug(order.toString());
              req = requests.get(Long.valueOf(order.getId()));
              if (req != null)
                req.set(order);
              else
                log.error("Unexpected {} event: {}", event.getEventType(), event);
              requests.remove(Long.valueOf(order.getId()));
              break;
            case ERROR:
              if (payload instanceof OkCoinPlaceOrderError) {
                log.debug("Processed error for order placement");
                req = requests.get(OkCoinWebSocketAPIRequest.DEFAULT_REQUEST_ID);
                if (req != null)
                  req.set(false);
                else
                  log.error("Unexpected {} event: {}", event.getEventType(), event);
                requests.remove(((OkCoinCancelOrderError) payload).getOrderId());
              } else if (payload instanceof OkCoinCancelOrderError) {
                log.debug("Processed error for {}", ((OkCoinCancelOrderError) payload).getOrderId());
                long orderId = ((OkCoinCancelOrderError) payload).getOrderId();
                req = requests.get(orderId);
                if (req != null)
                  req.set(false);
                else
                  log.error("Unexpected {} event: {}", event.getEventType(), event);
                requests.remove(((OkCoinCancelOrderError) payload).getOrderId());

              }
              if (payload instanceof OkCoinGetOrderInfoError) {
                log.debug("Processed error for {}", ((OkCoinGetOrderInfoError) payload).getOrderId());
                long orderId = ((OkCoinGetOrderInfoError) payload).getOrderId();
                req = requests.get(orderId);
                if (req != null)
                  req.set(null);
                else
                  log.error("Unexpected {} event: {}", event.getEventType(), event);
                requests.remove(((OkCoinGetOrderInfoError) payload).getOrderId());
              } else
                log.error("Unprocessed error: {}", event.toString());
              break;
            case DISCONNECT:
              break;
            default:
              log.debug("Unprocessed {} event: {}", event.getEventType(), event);
              break;
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    });
    super.connect();
  }

  @Override
  public void disconnect() {
    executor.shutdown();
    super.disconnect();
  }

}
