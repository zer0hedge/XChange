package com.xeiam.xchange.okcoin.service.streaming;

import static org.fest.assertions.api.Assertions.assertThat;

import org.java_websocket.WebSocket;
import org.junit.Ignore;
import org.junit.Test;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeFactory;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.okcoin.OkCoinExchange;
import com.xeiam.xchange.service.streaming.ExchangeEvent;
import com.xeiam.xchange.service.streaming.ExchangeEventType;
import com.xeiam.xchange.service.streaming.StreamingMarketDataService;

/**
 * @author timmolter
 */
public class TickerStreamingIntegration {

  @Test
  @Ignore // too slow
  public void tickerFetchStreamingChinaTest() throws Exception {
    ExchangeSpecification exSpec = new ExchangeSpecification(OkCoinExchange.class);
    exSpec.setExchangeSpecificParametersItem("Use_Intl", false);

    Exchange exchange = ExchangeFactory.INSTANCE.createExchange(exSpec);
    final StreamingMarketDataService service = exchange.getStreamingMarketDataService(new OkCoinStreamingConfiguration());

    service.connect();
    service.addTickerChannel(CurrencyPair.BTC_CNY);
    
    boolean gotTicker = false;

    while (!gotTicker) {
      assertThat(service.getWebSocketStatus()).isNotEqualTo(WebSocket.READYSTATE.CLOSED);

      ExchangeEvent event = service.getNextEvent();

      if (event != null) {
        if (event.getEventType().equals(ExchangeEventType.TICKER)) {
          gotTicker = true;
          Ticker ticker = (Ticker) event.getPayload();
          System.out.println(ticker.toString());
          assertThat(ticker).isNotNull();
        }
      }
    }

    service.disconnect();
  }

  @Test
  public void tickerFetchStreamingIntlTest() throws Exception {
    ExchangeSpecification exSpec = new ExchangeSpecification(OkCoinExchange.class);
    exSpec.setExchangeSpecificParametersItem("Use_Intl", true);

    Exchange exchange = ExchangeFactory.INSTANCE.createExchange(exSpec);

    //This constructor hack has AIDS
    final StreamingMarketDataService service = exchange
        .getStreamingMarketDataService(new OkCoinStreamingConfiguration(new CurrencyPair[] { CurrencyPair.BTC_USD }));

    service.connect();
    service.addTickerChannel(CurrencyPair.BTC_USD);

    boolean gotTicker = false;

    while (!gotTicker) {
      assertThat(service.getWebSocketStatus()).isNotEqualTo(WebSocket.READYSTATE.CLOSED);

      ExchangeEvent event = service.getNextEvent();

      if (event != null) {
        if (event.getEventType().equals(ExchangeEventType.TICKER)) {
          gotTicker = true;
          //Ticker ticker = (Ticker) event.getPayload();
          //System.out.println(ticker.toString());
          //assertThat(ticker).isNotNull();
        }
      }
    }

    service.disconnect();
  }

}
