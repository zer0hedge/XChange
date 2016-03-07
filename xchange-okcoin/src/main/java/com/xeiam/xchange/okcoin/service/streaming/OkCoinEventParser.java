package com.xeiam.xchange.okcoin.service.streaming;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.okcoin.OkCoinAdapters;
import com.xeiam.xchange.okcoin.dto.marketdata.OkCoinDepth;
import com.xeiam.xchange.okcoin.dto.marketdata.OkCoinStreamingDepth;
import com.xeiam.xchange.okcoin.dto.marketdata.OkCoinStreamingTicker;
import com.xeiam.xchange.okcoin.dto.marketdata.OkCoinTickerResponse;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinCancelOrderError;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinGetOrderInfoError;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrdersResult;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinPlaceOrderError;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinTradeResult;
import com.xeiam.xchange.service.streaming.ExchangeEvent;
import com.xeiam.xchange.service.streaming.ExchangeEventType;

class OkCoinEventParser {
  private final ObjectMapper mapper = new ObjectMapper();
  private final JsonFactory jsonFactory = new JsonFactory();
  private final BlockingQueue<ExchangeEvent> eventQueue;
  private final CurrencyPair[] currencyPairs;
  private final ChannelProvider channelProvider;

  private final Logger log = LoggerFactory.getLogger(OkCoinEventParser.class);

  OkCoinEventParser(BlockingQueue<ExchangeEvent> eventQueue, ChannelProvider channelProvider,
      CurrencyPair[] currencyPairs) {
    this.eventQueue = eventQueue;
    this.channelProvider = channelProvider;
    this.currencyPairs = currencyPairs;
  }

  public void onReceive(String msg) throws JsonParseException, JsonMappingException, IOException {
    log.debug("Received message: " + msg);
    JsonParser parser = jsonFactory.createParser(msg);
    if (parser.nextToken() == JsonToken.START_ARRAY) {
      ArrayNode readTree = (ArrayNode) mapper.readTree(msg);

      Iterator<JsonNode> iterator = readTree.iterator();
      while (iterator.hasNext()) {
        JsonNode node = iterator.next();

        // parse any requested channels
        for (int i = 0; i < currencyPairs.length; i++) {
          parseExchangeEvent(node, currencyPairs[i]);
        }
      }
    } else {
      // only pong should be here
    }
  }

  private void parseExchangeEvent(JsonNode node, CurrencyPair currencyPair)
      throws JsonParseException, JsonMappingException, IOException {

    if (node.get("channel").textValue().equals(channelProvider.getDepth20(currencyPair))) {
      parseDepth(node, currencyPair);

    } else if (node.get("channel").textValue().equals(channelProvider.getTrades(currencyPair))) {
      parseTrades(node, currencyPair);

    } else if (node.get("channel").textValue().equals(channelProvider.getTicker(currencyPair))) {
      parseTicker(node, currencyPair);

    } else if (node.get("channel").textValue().equals(channelProvider.getPlaceLimitOrder())) {
      parseLimitOrderPlacementResponse(node);

    } else if (node.get("channel").textValue().equals(channelProvider.getCancelOrder())) {
      parseCancelOrderResponse(node);

    } else if (node.get("channel").textValue().equals(channelProvider.getOrderInfo())) {
      parseOrderInfoResponse(node);
    }

  }

  private void parseOrderInfoResponse(JsonNode node) throws JsonParseException, JsonMappingException, IOException {

    if (!node.has("errorcode")) {
      OkCoinOrdersResult result = mapper.readValue(node.get("data").toString(), OkCoinOrdersResult.class);
      putEvent(ExchangeEventType.USER_ORDER, result);
    } else {
      putEvent(ExchangeEventType.ERROR,
          new OkCoinGetOrderInfoError(false, node.get("errorcode").asInt(), node.get("order_id").asLong()));
    }
  }

  private void parseCancelOrderResponse(JsonNode node) throws JsonParseException, JsonMappingException, IOException {

    if (!node.has("errorcode")) {
      OkCoinTradeResult result = mapper.readValue(node.get("data").toString(), OkCoinTradeResult.class);
      putEvent(ExchangeEventType.ORDER_CANCELED, result);
    } else if (node.get("errorcode").asInt() == 10050) {
      // Exchange for some reason does not provide orderId's on errors
      OkCoinTradeResult result = new OkCoinTradeResult(true, 0, -1);
      putEvent(ExchangeEventType.ORDER_CANCELED, result);
    } else {
      putEvent(ExchangeEventType.ERROR,
          new OkCoinCancelOrderError(false, node.get("errorcode").asInt(), node.get("order_id").asLong()));
    }
  }

  private void parseLimitOrderPlacementResponse(JsonNode node)
      throws JsonParseException, JsonMappingException, IOException {

    if (!node.has("errorcode")) {
      OkCoinTradeResult result = mapper.readValue(node.get("data").toString(), OkCoinTradeResult.class);
      putEvent(ExchangeEventType.ORDER_ADDED, result);
    } else {
      putEvent(ExchangeEventType.ERROR, new OkCoinPlaceOrderError(false, node.get("errorcode").asInt(), -1L));
    }
  }

  private void parseTicker(JsonNode node, CurrencyPair currencyPair)
      throws IOException, JsonParseException, JsonMappingException {

    OkCoinStreamingTicker ticker = mapper.readValue(node.get("data").toString(), OkCoinStreamingTicker.class);
    OkCoinTickerResponse tickerResponse = new OkCoinTickerResponse(ticker);
    tickerResponse.setDate(ticker.getTimestamp());
    putEvent(ExchangeEventType.TICKER, OkCoinAdapters.adaptTicker(tickerResponse, currencyPair));
  }

  private void parseDepth(JsonNode node, CurrencyPair currencyPair)
      throws IOException, JsonParseException, JsonMappingException {
    if (node.get("data") != null) {
      OkCoinDepth depth = mapper.readValue(node.get("data").toString(), OkCoinStreamingDepth.class);
      putEvent(ExchangeEventType.DEPTH, OkCoinAdapters.adaptOrderBook(depth, currencyPair));
    }
  }

  private void parseTrades(JsonNode node, CurrencyPair currencyPair) {

    JsonNode jsonNode = node.get("data");
    for (int i = 0; i < jsonNode.size(); i++) {
      JsonNode trade = jsonNode.get(i);
      putEvent(ExchangeEventType.TRADE, OkCoinAdapters.adaptStreamingTrade(trade, currencyPair));
    }
  }

  private void putEvent(ExchangeEventType eventType, Object payload) {
    try {
      eventQueue.put(new OkCoinExchangeEvent(eventType, payload));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void onDisconnect() {

    putEvent(ExchangeEventType.DISCONNECT, new Object());
  }
}
