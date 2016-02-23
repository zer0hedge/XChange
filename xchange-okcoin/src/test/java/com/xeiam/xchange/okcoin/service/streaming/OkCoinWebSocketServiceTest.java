package com.xeiam.xchange.okcoin.service.streaming;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrdersResult;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinTradeResult;
import com.xeiam.xchange.service.streaming.ExchangeEvent;

public class OkCoinWebSocketServiceTest {

  OkCoinEventParser sut;
  BlockingQueue<ExchangeEvent> eventQueue = new ArrayBlockingQueue<>(10);
  private SpotChannelProvider channelProvider;

  @Before
  public void setUp() {
    channelProvider = mock(SpotChannelProvider.class);
    sut = new OkCoinEventParser(eventQueue, channelProvider, new CurrencyPair[] { CurrencyPair.BTC_USD });
  }

  @Test
  public void receiveOrderPlacementResponseSuccessTest()
      throws InterruptedException, JsonParseException, JsonMappingException, IOException {
    String responseMsg = "[{'channel':'ok_spotusd_trade','data':{'order_id':'125433029','result':'true'}}]".replace("'",
        "\"");

    when(channelProvider.getPlaceLimitOrder()).thenReturn("ok_spotusd_trade");

    sut.onReceive(responseMsg);
    OkCoinTradeResult payload = (OkCoinTradeResult) eventQueue.take().getPayload();
    assertEquals(125433029L, payload.getOrderId());
  }

  @Test
  public void receiveOrderPlacementResponseFailTest()
      throws InterruptedException, JsonParseException, JsonMappingException, IOException {
    String responseMsg = "[{\"channel\":\"ok_spotusd_trade\",\"errorcode\":\"10002\",\"success\":\"false\"}]";

    when(channelProvider.getPlaceLimitOrder()).thenReturn("ok_spotusd_trade");

    sut.onReceive(responseMsg);
    OkCoinTradeResult payload = (OkCoinTradeResult) eventQueue.take().getPayload();
    assertEquals(10002, payload.getErrorCode());
  }

  @Test
  public void receiveOrderCancellationResponseSuccessTest()
      throws InterruptedException, JsonParseException, JsonMappingException, IOException {
    String responseMsg = "[{'channel':'ok_spotusd_cancel_order','data':{'order_id':'125433029','result':'true'}}]"
        .replace("'", "\"");

    when(channelProvider.getCancelOrder()).thenReturn("ok_spotusd_cancel_order");

    sut.onReceive(responseMsg);
    OkCoinTradeResult payload = (OkCoinTradeResult) eventQueue.take().getPayload();
    assertEquals(125433029L, payload.getOrderId());
  }

  @Test
  public void receiveOrderCancellationResponseFailTest()
      throws InterruptedException, JsonParseException, JsonMappingException, IOException {
    String responseMsg = "[{\"channel\":\"ok_spotusd_cancel_order\",\"errorcode\":\"10009\",\"order_id\":\"100\", \"success\":\"false\"}]";

    when(channelProvider.getCancelOrder()).thenReturn("ok_spotusd_cancel_order");

    sut.onReceive(responseMsg);
    OkCoinTradeResult payload = (OkCoinTradeResult) eventQueue.take().getPayload();
    assertEquals(10009, payload.getErrorCode());
  }

  @Test
  public void receiveOrderResponseSuccessTest()
      throws InterruptedException, JsonParseException, JsonMappingException, IOException {
    String responseMsg = "[{\"channel\": \"ok_spotusd_order_info\","
        + "\"data\": "
        + "{\"orders\": "
        + "[{\"amount\": 0.1,\"avg_price\": 1.961,\"create_date\": 1422502117000,\"deal_amount\": 0.1,\"order_id\": 20914907,\"orders_id\": 20914907,\"price\": 0,\"status\": 2,\"symbol\": \"ltc_usd\",\"type\": \"sell_market\"}],\"result\": true}}]"
        .replace("'", "\"");

    when(channelProvider.getOrderInfo()).thenReturn("ok_spotusd_order_info");

    sut.onReceive(responseMsg);

    OkCoinOrdersResult payload = (OkCoinOrdersResult) eventQueue.take().getPayload();

    assertEquals(20914907L, payload.getOrders()[0].getOrderId());
  }
  
  @Test
  public void receiveOrderResponseFailTest()
      throws InterruptedException, JsonParseException, JsonMappingException, IOException {
    String responseMsg = "[{\"channel\": \"ok_spotusd_order_info\","
        + "\"data\": "
        + "{\"orders\": "
        + "[{\"amount\": 0.1,\"avg_price\": 1.961,\"create_date\": 1422502117000,\"deal_amount\": 0.1,\"order_id\": 20914907,\"orders_id\": 20914907,\"price\": 0,\"status\": 2,\"symbol\": \"ltc_usd\",\"type\": \"sell_market\"}],\"result\": true}}]"
        .replace("'", "\"");

    when(channelProvider.getOrderInfo()).thenReturn("ok_spotusd_order_info");

    sut.onReceive(responseMsg);

    OkCoinOrdersResult payload = (OkCoinOrdersResult) eventQueue.take().getPayload();

    assertEquals(20914907L, payload.getOrders()[0].getOrderId());
  }

}
