package com.xeiam.xchange.okcoin.service.streaming;

import static org.junit.Assert.assertEquals;
import static org.fest.assertions.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;
import com.xeiam.xchange.okcoin.OkCoinExchange;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinCancelOrderError;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinGetOrderInfoError;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrder;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrdersResult;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinPlaceOrderError;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinTradeResult;
import com.xeiam.xchange.service.streaming.ExchangeEventType;

public class OkCoinStreamingTradeServiceTest {

  public OkCoinStreamingTradeService sut;
  
  LimitOrder mockLimitOrder() {
		LimitOrder limitOrder = mock(LimitOrder.class);
	    when(limitOrder.getCurrencyPair()).thenReturn(CurrencyPair.BTC_USD);
	    when(limitOrder.getLimitPrice()).thenReturn(BigDecimal.TEN);
	    when(limitOrder.getTradableAmount()).thenReturn(BigDecimal.TEN);
		return limitOrder;
	}


  @Before
  public void setUp() {

    Exchange okcoin = mock(OkCoinExchange.class);
    ExchangeSpecification exchangeSpecification = mock(ExchangeSpecification.class);
    when(exchangeSpecification.getExchangeSpecificParametersItem("Websocket_SslUri")).thenReturn("");
    when(exchangeSpecification.getExchangeSpecificParametersItem("Use_Futures")).thenReturn(false);
    when(exchangeSpecification.getExchangeSpecificParametersItem("Use_Intl")).thenReturn(true);
    when(okcoin.getExchangeSpecification()).thenReturn(exchangeSpecification);
    okcoin.applySpecification(exchangeSpecification);
    OkCoinStreamingConfiguration config = mock(OkCoinStreamingConfiguration.class);
    WebSocketBase socketBase = mock(WebSocketBase.class);

    sut = new OkCoinStreamingTradeService(okcoin, config);
    sut.setSocketBase(socketBase);
    sut.connect();
  }
  
  @After
  public void tearDown() {
	  sut.disconnect();
  }

  @Test
  public void testPlaceLimitOrder() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
      ExchangeException, IOException, InterruptedException, ExecutionException {

    LimitOrder limitOrder = mockLimitOrder();

    Future<String> order = sut.placeLimitOrderNonBlocking(limitOrder);
    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100)));
    assertEquals("100", order.get());

  }

  @Test(expected = ExchangeException.class)
  public void testPlaceLimitOrderFailure() throws NotAvailableFromExchangeException,
      NotYetImplementedForExchangeException, ExchangeException, IOException, InterruptedException, ExecutionException {

    LimitOrder limitOrder = mockLimitOrder();

    Future<String> order = sut.placeLimitOrderNonBlocking(limitOrder);
    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ERROR, new OkCoinPlaceOrderError(false, 10016, 0)));
    order.get();
  }

  @Test
  public void testCancelOrder() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
      ExchangeException, IOException, InterruptedException, ExecutionException {

    LimitOrder limitOrder = mockLimitOrder();

    Future<String> order = sut.placeLimitOrderNonBlocking(limitOrder);
    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100)));
    String id = order.get();

    Future<Boolean> r = sut.cancelOrderNonBlocking(id, CurrencyPair.BTC_USD);
    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_CANCELED, new OkCoinTradeResult(true, 0, 100)));
    assertThat(r.get()).isTrue();

  }

  @Test(expected = ExchangeException.class)
  public void testCancelOrderFailure() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
      ExchangeException, IOException, InterruptedException, ExecutionException {

    LimitOrder limitOrder = mockLimitOrder();
    
    Future<String> order = sut.placeLimitOrderNonBlocking(limitOrder);
    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100)));
    String id = order.get();
    
    Future<Boolean> r = sut.cancelOrderNonBlocking(id, CurrencyPair.BTC_USD);
    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ERROR, new OkCoinCancelOrderError(false, 10009, Long.valueOf(id))));
    r.get();
    

  }
  

  @Test
  public void testGetOrder() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
      ExchangeException, IOException, InterruptedException, ExecutionException {

    LimitOrder limitOrder = mockLimitOrder();
    
    Future<String> order = sut.placeLimitOrderNonBlocking(limitOrder);

    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100)));
    String id = order.get();

    OkCoinOrder serverOrder = new OkCoinOrder(100L, 1, "btc_usd", "", limitOrder.getLimitPrice(),
        limitOrder.getLimitPrice(), limitOrder.getTradableAmount(), limitOrder.getTradableAmount().divide(new BigDecimal("2")),
        null);
    Future<LimitOrder> orderInfo = sut.getOrderNonBlocking(id, CurrencyPair.BTC_USD);
    sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.USER_ORDER,
        new OkCoinOrdersResult(true, 0, new OkCoinOrder[] { serverOrder })));
    LimitOrder response = orderInfo.get();
    
    assertEquals(id, response.getId());
    assertEquals(limitOrder.getTradableAmount().divide(new BigDecimal("2")), response.getTradableAmount());

  }


  @Test(expected=ExchangeException.class)
  public void testGetOrderFailure() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
      ExchangeException, IOException, InterruptedException, ExecutionException {

    LimitOrder limitOrder = mockLimitOrder();
    
    Future<String> order = sut.placeLimitOrderNonBlocking(limitOrder);

    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100)));
    String id = order.get();
    
    Future<LimitOrder> orderInfo = sut.getOrderNonBlocking(id, CurrencyPair.BTC_USD);

    sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.ERROR,
        new OkCoinGetOrderInfoError(false, 10002,  Long.valueOf(id))));
    orderInfo.get();

  }

}
