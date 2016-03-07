package com.xeiam.xchange.okcoin.service.streaming;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.xchange.Exchange;
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
import com.xeiam.xchange.service.streaming.ExchangeEventType;
import com.xeiam.xchange.service.streaming.ExchangeStreamingConfiguration;
import com.xeiam.xchange.service.streaming.StreamingTradeService;

public class OkCoinStreamingTradeService extends OkCoinBaseStreamingService implements StreamingTradeService {

  private String apikey;
  private OkCoinDigest signatureCreator;
  private final BlockingQueue<OkCoinWebSocketAPIRequest> newRequestsQueue = new LinkedBlockingQueue<OkCoinWebSocketAPIRequest>();
  private Lock placeOrderLock = new ReentrantLock();
  private Lock cancelOrderLock = new ReentrantLock();
  private Lock orderInfoLock = new ReentrantLock();

  public OkCoinStreamingTradeService(Exchange exchange, ExchangeStreamingConfiguration exchangeStreamingConfiguration) {
    super(exchange, exchangeStreamingConfiguration, "TS");
    apikey = exchange.getExchangeSpecification().getApiKey();
    signatureCreator = new OkCoinDigest(apikey, exchange.getExchangeSpecification().getSecretKey());
  }

  @Override
  public String placeLimitOrder(LimitOrder limitOrder) {

    placeOrderLock.lock();

    while (true) {
      try {
        OkCoinPlaceLimitOrderRequest request = new OkCoinPlaceLimitOrderRequest(limitOrder, channelProvider, apikey,
            signatureCreator);
        newRequestsQueue.put(request);
        String orderId = request.get();
        knownOrders.put(orderId, limitOrder);

        placeOrderLock.unlock();
        return orderId;
      } catch (ExecutionException e) {
        log.warn(e.getCause().getMessage());
        continue;
      } catch (InterruptedException e) {
        log.error("Unable to place order", e);
        placeOrderLock.unlock();
        return null;
      } catch (ExchangeException e) {
        placeOrderLock.unlock();
        throw e;
      }
    }

  }

  @Override
  public void cancelOrder(String orderId)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    cancelOrderLock.lock();

    while (true) {
      try {
        OkCoinCancelOrderRequest request = new OkCoinCancelOrderRequest(orderId,
            knownOrders.get(orderId).getCurrencyPair().toString().replace("/", "_").toLowerCase(), channelProvider,
            apikey, signatureCreator);
        newRequestsQueue.put(request);
        request.get();

        cancelOrderLock.unlock();
        return;
      } catch (InterruptedException e) {
        log.error("Unable to cancel order", e);
        cancelOrderLock.unlock();
        return;
      } catch (ExecutionException e) {
        log.warn(e.getCause().getMessage());
        continue;
      } catch (ExchangeException e) {
        cancelOrderLock.unlock();
        throw e;
      }
    }
  }

  @Override
  public LimitOrder getOrder(String orderId)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException {

    orderInfoLock.lock();
    while (true) {
      try {
        OkCoinGetOrderInfoRequest request = new OkCoinGetOrderInfoRequest(orderId,
            knownOrders.get(orderId).getCurrencyPair().toString().replace("/", "_").toLowerCase(), channelProvider,
            apikey, signatureCreator);
        newRequestsQueue.put(request);
        LimitOrder response = request.get();
        orderInfoLock.unlock();
        return response;
      } catch (InterruptedException e) {
        log.error("Unable to cancel order", e);
        orderInfoLock.unlock();
        return null;
      } catch (ExecutionException e) {
        log.warn(e.getCause().getMessage());
        continue;
      } catch (ExchangeException e) {
        orderInfoLock.unlock();
        throw e;
      }
    }
  }

  private ConcurrentMap<String, LimitOrder> knownOrders = new ConcurrentHashMap<>();
  private RequestStore requests = new RequestStore();
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
            requests.put(request);
            log.debug("Waiting for {}", request.getIdentifier());
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
              requests.processResponse(event, new RequestIdentifier(event.getEventType()), (OkCoinTradeResult) payload);
              break;
            case ORDER_CANCELED:
              requests.processResponse(event, new RequestIdentifier(event.getEventType()), true);
              break;
            case USER_ORDER:
              LimitOrder order = OkCoinAdapters.adaptOrder(((OkCoinOrdersResult) payload).getOrders()[0]);
              log.debug(order.toString());
              requests.processResponse(event, new RequestIdentifier(event.getEventType()), order);
              break;
            case ERROR:
              if (payload instanceof OkCoinPlaceOrderError) {
                requests.processResponse(event, new RequestIdentifier(ExchangeEventType.ORDER_ADDED), payload);
              } else if (payload instanceof OkCoinCancelOrderError) {
                requests.processResponse(event, new RequestIdentifier(ExchangeEventType.ORDER_CANCELED), false);
              } else if (payload instanceof OkCoinGetOrderInfoError) {
                requests.processResponse(event, new RequestIdentifier(ExchangeEventType.USER_ORDER), null);
              } else
                log.error("Unprocessed error: {}", event.toString());
              break;
            case DISCONNECT:
              log.warn("Disconnect event arrived!");
              requests.broadcastDisconnection();
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
