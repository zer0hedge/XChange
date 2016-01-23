package com.xeiam.xchange.okcoin.service.streaming.trade;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;
import com.xeiam.xchange.okcoin.OkCoinExchange;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrder;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrdersResult;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinTradeResult;
import com.xeiam.xchange.okcoin.service.streaming.OkCoinExchangeEvent;
import com.xeiam.xchange.okcoin.service.streaming.OkCoinStreamingConfiguration;
import com.xeiam.xchange.okcoin.service.streaming.WebSocketBase;
import com.xeiam.xchange.service.streaming.ExchangeEventType;

public class OkCoinStreamingTradeServiceTest {

  public OkCoinStreamingTradeService sut;

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
  }

  @Test
  public void testPlaceLimitOrder() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
      ExchangeException, IOException, InterruptedException {

    LimitOrder limitOrder = mock(LimitOrder.class);
    when(limitOrder.getCurrencyPair()).thenReturn(CurrencyPair.BTC_USD);
    when(limitOrder.getLimitPrice()).thenReturn(BigDecimal.TEN);
    when(limitOrder.getTradableAmount()).thenReturn(BigDecimal.TEN);

    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100)));
    assertEquals("100", sut.placeLimitOrder(limitOrder));

  }

  @Test(expected = ExchangeException.class)
  public void testPlaceLimitOrderFailure() throws NotAvailableFromExchangeException,
      NotYetImplementedForExchangeException, ExchangeException, IOException, InterruptedException {

    LimitOrder limitOrder = mock(LimitOrder.class);
    when(limitOrder.getCurrencyPair()).thenReturn(CurrencyPair.BTC_USD);
    when(limitOrder.getLimitPrice()).thenReturn(BigDecimal.TEN);
    when(limitOrder.getTradableAmount()).thenReturn(BigDecimal.TEN);

    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ERROR, new OkCoinTradeResult(false, 10016, 0)));
    sut.placeLimitOrder(limitOrder);
  }

  @Test
  public void testCancelOrder() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
      ExchangeException, IOException, InterruptedException {

    LimitOrder limitOrder = mock(LimitOrder.class);
    when(limitOrder.getCurrencyPair()).thenReturn(CurrencyPair.BTC_USD);
    when(limitOrder.getLimitPrice()).thenReturn(BigDecimal.TEN);
    when(limitOrder.getTradableAmount()).thenReturn(BigDecimal.TEN);

    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100)));
    String id = sut.placeLimitOrder(limitOrder);

    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_CANCELED, new OkCoinTradeResult(true, 0, 100)));
    sut.cancelOrder(id);

  }

  @Test(expected = ExchangeException.class)
  public void testCancelOrderFailure() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
      ExchangeException, IOException, InterruptedException {

    LimitOrder limitOrder = mock(LimitOrder.class);
    when(limitOrder.getCurrencyPair()).thenReturn(CurrencyPair.BTC_USD);
    when(limitOrder.getLimitPrice()).thenReturn(BigDecimal.TEN);
    when(limitOrder.getTradableAmount()).thenReturn(BigDecimal.TEN);

    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ERROR, new OkCoinTradeResult(false, 10016, 0)));
    String id = sut.placeLimitOrder(limitOrder);

    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_CANCELED, new OkCoinTradeResult(false, 10009, 0)));
    sut.cancelOrder(id);
  }

  @Test
  public void testGetOrder() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
      ExchangeException, IOException, InterruptedException {

    LimitOrder limitOrder = mock(LimitOrder.class);
    when(limitOrder.getCurrencyPair()).thenReturn(CurrencyPair.BTC_USD);
    when(limitOrder.getLimitPrice()).thenReturn(BigDecimal.TEN);
    when(limitOrder.getTradableAmount()).thenReturn(BigDecimal.TEN);

    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100)));
    String id = sut.placeLimitOrder(limitOrder);

    OkCoinOrder serverOrder = new OkCoinOrder(100L, 1, "btc_usd", "", limitOrder.getLimitPrice(),
        limitOrder.getLimitPrice(), limitOrder.getTradableAmount(), limitOrder.getTradableAmount().divide(new BigDecimal("2")),
        null);

    sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.USER_ORDER,
        new OkCoinOrdersResult(true, 0, new OkCoinOrder[] { serverOrder })));
    LimitOrder response = sut.getOrder(id);
    assertEquals(id, response.getId());
    assertEquals(limitOrder.getTradableAmount().divide(new BigDecimal("2")), response.getTradableAmount());

  }

  @Test(expected=ExchangeException.class)
  public void testGetOrderFail() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
      ExchangeException, IOException, InterruptedException {

    LimitOrder limitOrder = mock(LimitOrder.class);
    when(limitOrder.getCurrencyPair()).thenReturn(CurrencyPair.BTC_USD);
    when(limitOrder.getLimitPrice()).thenReturn(BigDecimal.TEN);
    when(limitOrder.getTradableAmount()).thenReturn(BigDecimal.TEN);

    sut.getMarketDataEventQueue()
        .put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100)));
    String id = sut.placeLimitOrder(limitOrder);

    sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.ERROR,
        new OkCoinOrdersResult(false, 10002, null)));
    sut.getOrder(id).getId();

  }

}
