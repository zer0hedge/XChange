package com.xeiam.xchange.okcoin.service.polling.marketdata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.OrderBook;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;
import com.xeiam.xchange.okcoin.OkCoinExchange;

public class DepthPollingTest {

  @Test
  public void pollingDepthTest()
      throws NotAvailableFromExchangeException, NotYetImplementedForExchangeException, ExchangeException, IOException {
    ExchangeSpecification exSpec = new ExchangeSpecification(OkCoinExchange.class);
    exSpec.setExchangeSpecificParametersItem("Use_Intl", true);

    Exchange exchange = ExchangeFactory.INSTANCE.createExchange(exSpec);
    OrderBook orderBook = exchange.getPollingMarketDataService().getOrderBook(CurrencyPair.BTC_USD, 20);
    assertEquals(20, orderBook.getAsks().size());
    assertEquals(20, orderBook.getBids().size());
    
    orderBook = exchange.getPollingMarketDataService().getOrderBook(CurrencyPair.BTC_USD);
    // Just some number it should be larger than
    assertTrue(orderBook.getAsks().size()>20);
    assertTrue(orderBook.getBids().size()>20);
    
    // Check orders order
    assertEquals(-1,orderBook.getAsks().get(0).getLimitPrice().compareTo(orderBook.getAsks().get(1).getLimitPrice()));
    assertEquals(1,orderBook.getBids().get(0).getLimitPrice().compareTo(orderBook.getBids().get(1).getLimitPrice()));

  }

}
