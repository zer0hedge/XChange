package com.xeiam.xchange.okcoin.service.streaming;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;
import com.xeiam.xchange.okcoin.OkCoinExchange;

/**
 * @author zer0hedge
 * 
 * These integration tests are to be used in MANUAL mode, i.e. Maven should ignore them
 * To run the tests:
 * 		1. Put keys.json into ~/exchanges/okcoin directory
 * 		2. Change limitPrice value below so you are sure that ASK will NOT execute at that price
 * 		3. Check your OkCoin account balance - it must exceed 0.01 BTC 
 * 	    4. Remove @Ignore below and then run the tests in your IDE or by Maven 
 *
 */
@Ignore
public class TradeAPIIntegration {

	// Change it!
	private BigDecimal limitPrice  = new BigDecimal("386");;

	
	
	private static CurrencyPair currencyPair;
	private static String path;
	private Exchange exchange;
	private OkCoinStreamingConfiguration configuration;
	private OkCoinStreamingTradeService sut;	// System Under Test

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		currencyPair = CurrencyPair.BTC_USD;
		path = System.getProperty("user.home") + File.separator + "exchanges/okcoin/keys.json";
	}

	@Before
	public void setUp() throws Exception {
		JsonFactory jsonFactory = new JsonFactory();
		JsonParser keyParser = jsonFactory.createParser(IOUtils.toString((new FileInputStream(path))));
		String key = null;
		String secret = null;
		if (keyParser.nextToken() != JsonToken.START_OBJECT) {
			throw new IOException("Expected data to start with an Object");
		}
		while (keyParser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = keyParser.getCurrentName();
			// Let's move to value
			keyParser.nextToken();
			if (fieldName.equals("key")) {
				key = keyParser.getText();
			}
			if (fieldName.equals("secret")) {
				secret = keyParser.getText();
			}
		}
		assertThat(key).isNotNull();
		assertThat(secret).isNotNull();
		
		exchange = ExchangeFactory.INSTANCE.createExchange(OkCoinExchange.class.getName());
		ExchangeSpecification spec = exchange.getDefaultExchangeSpecification();
		spec.setApiKey(key);
		spec.setSecretKey(secret);
		spec.setExchangeSpecificParametersItem("Use_Intl", true);
		exchange.applySpecification(spec);
		configuration = new OkCoinStreamingConfiguration(new CurrencyPair[] { currencyPair });
		sut = (OkCoinStreamingTradeService) exchange.getStreamingTradeService(configuration);
		sut.connect();

	}

	@After
	public void tearDown() throws Exception {
		sut.disconnect();
	}
	
	@Test
	public void shouldPlaceOrderAndGetInfoAndCancelIt() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException, InterruptedException, ExecutionException, TimeoutException {
		// Set limit price below appropriately, so order will NOT get executed until it is cancelled by the test !
		// If the test fails, you have to CANCEL ORDER MANUALLY !
		
		LimitOrder limitOrder = new LimitOrder(OrderType.ASK, new BigDecimal("0.01"), currencyPair, "1", null, limitPrice);
		Future<String> orderId = sut.placeLimitOrderNonBlocking(limitOrder);
		assertThat(orderId.get()).isNotEqualTo("-1");
		
		Future<LimitOrder> orderInfo = sut.getOrderNonBlocking(orderId.get(), currencyPair);
		LimitOrder o = orderInfo.get();
		assertThat(o.getId()).isEqualTo(orderId.get());

		Future<Boolean> isCancelled = sut.cancelOrderNonBlocking(orderId.get(), currencyPair);
		assertThat(isCancelled.get()).isTrue();
		
	}


	@Test(expected = ExchangeException.class)
	public void shouldNotCancelNonexistentOrder() throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException, InterruptedException, ExecutionException, TimeoutException {
		Future<Boolean> result = sut.cancelOrderNonBlocking("1", currencyPair);
		result.get();
	}

}
