package com.xeiam.xchange.okcoin.service.streaming;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinTradeResult;
import com.xeiam.xchange.service.streaming.ExchangeEvent;

import static org.mockito.Mockito.*;

public class OkCoinWebSocketServiceTest {
  
  OkCoinWebSocketService sut;
  BlockingQueue<ExchangeEvent> eventQueue = new ArrayBlockingQueue<>(10);
  private SpotChannelProvider channelProvider;
  
  @Before
  public void setUp() {
    channelProvider = mock(SpotChannelProvider.class);
    sut = new OkCoinWebSocketService(eventQueue, channelProvider, new CurrencyPair[] { CurrencyPair.BTC_USD });
  }
  
  @Test
  public void receiveOrderPlacementResponseSuccessTest() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
    String responseMsg = "[{'channel':'ok_spotusd_trade','data':{'order_id':'125433029','result':'true'}}]".replace("'", "\"");
    
    when(channelProvider.getPlaceLimitOrder()).thenReturn("ok_spotusd_trade");

    sut.onReceive(responseMsg);
    OkCoinTradeResult payload = (OkCoinTradeResult) eventQueue.take().getPayload();
    assertEquals(125433029L, payload.getOrderId());
  }
  
  @Test
  public void receiveOrderPlacementResponseFailTest() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
    String responseMsg = "[{\"channel\":\"ok_spotusd_trade\",\"errorcode\":\"10002\",\"success\":\"false\"}]";
    
    when(channelProvider.getPlaceLimitOrder()).thenReturn("ok_spotusd_trade");

    sut.onReceive(responseMsg);
    OkCoinTradeResult payload = (OkCoinTradeResult) eventQueue.take().getPayload();
    assertEquals(10002, payload.getErrorCode());
  }
  
  @Test
  public void receiveOrderCancellationResponseSuccessTest() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
    String responseMsg = "[{'channel':'ok_spotusd_cancel_order','data':{'order_id':'125433029','result':'true'}}]".replace("'", "\"");
    
    when(channelProvider.getCancelOrder()).thenReturn("ok_spotusd_cancel_order");

    sut.onReceive(responseMsg);
    OkCoinTradeResult payload = (OkCoinTradeResult) eventQueue.take().getPayload();
    assertEquals(125433029L, payload.getOrderId());
  }
  
  @Test
  public void receiveOrderCancellationResponseFailTest() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
    String responseMsg = "[{\"channel\":\"ok_spotusd_cancel_order\",\"errorcode\":\"10009\",\"success\":\"false\"}]";
    
    when(channelProvider.getCancelOrder()).thenReturn("ok_spotusd_cancel_order");

    sut.onReceive(responseMsg);
    OkCoinTradeResult payload = (OkCoinTradeResult) eventQueue.take().getPayload();
    assertEquals(10009, payload.getErrorCode());
  }
  
}
