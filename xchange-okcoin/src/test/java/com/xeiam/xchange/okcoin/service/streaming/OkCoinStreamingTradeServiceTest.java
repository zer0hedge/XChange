package com.xeiam.xchange.okcoin.service.streaming;

import static org.fest.assertions.api.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.hamcrest.core.IsEqual;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
	private WebSocketBase socketBase;

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
		socketBase = mock(WebSocketBase.class);

		sut = new OkCoinStreamingTradeService(okcoin, config);
		sut.setSocketBase(socketBase);
		sut.connect();
	}

	@After
	public void tearDown() {
		sut.disconnect();
	}

	@SuppressWarnings("unchecked")
	void stubPlaceLimitOrderOkCoinResponse(final ExchangeEventType exchangeEventType, final Object payload) {
		doAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				if (invocation.getArgumentAt(0, String.class).equals(sut.channelProvider.getPlaceLimitOrder()))
					sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(exchangeEventType, payload));
				return null;
			}
		}).when(socketBase).addOneTimeChannel(anyString(), (Map<String, String>) anyObject());

	}

	@Test
	public void testPlaceLimitOrder() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
			ExchangeException, IOException, InterruptedException, ExecutionException {

		LimitOrder limitOrder = mockLimitOrder();
		stubPlaceLimitOrderOkCoinResponse(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100));

		assertEquals("100", sut.placeLimitOrder(limitOrder));

	}

	@Test(expected = ExchangeException.class)
	public void testPlaceLimitOrderFailure()
			throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException,
			IOException, InterruptedException, ExecutionException {

		LimitOrder limitOrder = mockLimitOrder();

		stubPlaceLimitOrderOkCoinResponse(ExchangeEventType.ERROR, new OkCoinPlaceOrderError(false, 10016, 0));

		sut.placeLimitOrder(limitOrder);
	}

	@Test
	public void testCancelOrder() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
			ExchangeException, IOException, InterruptedException, ExecutionException {

		LimitOrder limitOrder = mockLimitOrder();
		stubPlaceLimitOrderOkCoinResponse(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100));

		String id = sut.placeLimitOrder(limitOrder);

		Future<Boolean> r = sut.cancelOrderNonBlocking(id, CurrencyPair.BTC_USD);
		sut.getMarketDataEventQueue()
				.put(new OkCoinExchangeEvent(ExchangeEventType.ORDER_CANCELED, new OkCoinTradeResult(true, 0, 100)));
		assertThat(r.get()).isTrue();

	}

	@Test(expected = ExchangeException.class)
	public void testCancelOrderFailure()
			throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException,
			IOException, InterruptedException, ExecutionException {

		LimitOrder limitOrder = mockLimitOrder();
		stubPlaceLimitOrderOkCoinResponse(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100));

		String id = sut.placeLimitOrder(limitOrder);

	
		Future<Boolean> r = sut.cancelOrderNonBlocking(id, CurrencyPair.BTC_USD);
		sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.ERROR,
				new OkCoinCancelOrderError(false, 10009, Long.valueOf(id))));
		r.get();

	}

	@Test
	public void testGetOrder() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
			ExchangeException, IOException, InterruptedException, ExecutionException {

		LimitOrder limitOrder = mockLimitOrder();

		stubPlaceLimitOrderOkCoinResponse(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100));

		String id = sut.placeLimitOrder(limitOrder);

		OkCoinOrder serverOrder = new OkCoinOrder(100L, 1, "btc_usd", "", limitOrder.getLimitPrice(),
				limitOrder.getLimitPrice(), limitOrder.getTradableAmount(),
				limitOrder.getTradableAmount().divide(new BigDecimal("2")), null);
		Future<LimitOrder> orderInfo = sut.getOrderNonBlocking(id, CurrencyPair.BTC_USD);
		sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.USER_ORDER,
				new OkCoinOrdersResult(true, 0, new OkCoinOrder[] { serverOrder })));
		LimitOrder response = orderInfo.get();

		assertEquals(id, response.getId());
		assertEquals(limitOrder.getTradableAmount().divide(new BigDecimal("2")), response.getTradableAmount());

	}
	
	@Test(timeout=1000)
	public void shouldReturnTheSameInfoTwice() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
			ExchangeException, IOException, InterruptedException, ExecutionException {

		LimitOrder limitOrder = mockLimitOrder();

		stubPlaceLimitOrderOkCoinResponse(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100));

		String id = sut.placeLimitOrder(limitOrder);

		OkCoinOrder serverOrder = new OkCoinOrder(100L, 1, "btc_usd", "", limitOrder.getLimitPrice(),
				limitOrder.getLimitPrice(), limitOrder.getTradableAmount(),
				limitOrder.getTradableAmount().divide(new BigDecimal("2")), null);
		Future<LimitOrder> orderInfo = sut.getOrderNonBlocking(id, CurrencyPair.BTC_USD);
		Future<LimitOrder> orderInfo2 = sut.getOrderNonBlocking(id, CurrencyPair.BTC_USD);

		sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.USER_ORDER,
				new OkCoinOrdersResult(true, 0, new OkCoinOrder[] { serverOrder })));
		
		sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.USER_ORDER,
				new OkCoinOrdersResult(true, 0, new OkCoinOrder[] { serverOrder })));

		LimitOrder response = orderInfo.get();
		LimitOrder response2 = orderInfo2.get();
		
		assertThat(response).isEqualTo(response2);


	}
	
	
	@Test(timeout=1000)
	public void shouldReturnInfoThenCancel() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
			ExchangeException, IOException, InterruptedException, ExecutionException {

		LimitOrder limitOrder = mockLimitOrder();

		stubPlaceLimitOrderOkCoinResponse(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100));

		String id = sut.placeLimitOrder(limitOrder);

		OkCoinOrder serverOrder = new OkCoinOrder(100L, 1, "btc_usd", "", limitOrder.getLimitPrice(),
				limitOrder.getLimitPrice(), limitOrder.getTradableAmount(),
				limitOrder.getTradableAmount().divide(new BigDecimal("2")), null);
		Future<LimitOrder> orderInfo = sut.getOrderNonBlocking(id, CurrencyPair.BTC_USD);
		Future<Boolean> r = sut.cancelOrderNonBlocking(id, CurrencyPair.BTC_USD);

		sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.USER_ORDER,
				new OkCoinOrdersResult(true, 0, new OkCoinOrder[] { serverOrder })));
		
		sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.ERROR,
				new OkCoinCancelOrderError(false, 10009, Long.valueOf(id))));
		r.get();

		LimitOrder response = orderInfo.get();
		assertThat(response).isNotNull();
		assertThat(r.get()).isTrue();


	}



	@Test(expected = ExchangeException.class)
	public void testGetOrderFailure() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException,
			ExchangeException, IOException, InterruptedException, ExecutionException {

		LimitOrder limitOrder = mockLimitOrder();

		stubPlaceLimitOrderOkCoinResponse(ExchangeEventType.ORDER_ADDED, new OkCoinTradeResult(true, 0, 100));

		String id = sut.placeLimitOrder(limitOrder);

		Future<LimitOrder> orderInfo = sut.getOrderNonBlocking(id, CurrencyPair.BTC_USD);

		sut.getMarketDataEventQueue().put(new OkCoinExchangeEvent(ExchangeEventType.ERROR,
				new OkCoinGetOrderInfoError(false, 10002, Long.valueOf(id))));
		orderInfo.get();

	}

}
