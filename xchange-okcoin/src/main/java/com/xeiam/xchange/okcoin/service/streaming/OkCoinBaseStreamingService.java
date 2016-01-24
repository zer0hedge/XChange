package com.xeiam.xchange.okcoin.service.streaming;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.java_websocket.WebSocket.READYSTATE;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.okcoin.OkCoinExchange;
import com.xeiam.xchange.service.streaming.ExchangeEvent;
import com.xeiam.xchange.service.streaming.ExchangeStreamingConfiguration;
import com.xeiam.xchange.service.streaming.StreamingExchangeService;

public class OkCoinBaseStreamingService implements StreamingExchangeService {
  private WebSocketBase socketBase;
  protected final ChannelProvider channelProvider;
  private final BlockingQueue<ExchangeEvent> marketDataEventQueue = new LinkedBlockingQueue<ExchangeEvent>();

  private final OkCoinStreamingConfiguration exchangeStreamingConfiguration;

  public OkCoinBaseStreamingService(Exchange exchange, ExchangeStreamingConfiguration streamingConfiguration) {
    
    this.exchangeStreamingConfiguration = (OkCoinStreamingConfiguration) streamingConfiguration;

    ExchangeSpecification exchangeSpecification = exchange.getExchangeSpecification();
    String sslUri = (String) exchangeSpecification.getExchangeSpecificParametersItem("Websocket_SslUri");
    boolean useFutures = (Boolean) exchangeSpecification.getExchangeSpecificParametersItem("Use_Futures");

    channelProvider = useFutures ? new FuturesChannelProvider(OkCoinExchange.futuresContractOfConfig(exchangeSpecification))
        : new SpotChannelProvider(exchangeSpecification);
    WebSocketService socketService = new OkCoinWebSocketService(marketDataEventQueue,
        channelProvider,
        this.exchangeStreamingConfiguration.getMarketDataCurrencyPairs());
    
    socketBase = new WebSocketBase(sslUri, socketService);
  }

  @Override
  public void connect() {
    socketBase.start();
  }

  @Override
  public void disconnect() {
  }

  @Override
  public ExchangeEvent getNextEvent() throws InterruptedException {
    return marketDataEventQueue.take();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int countEventsAvailable() {
    return marketDataEventQueue.size();
  }

  @Override
  public void send(String msg) {
  }

  @Override
  public READYSTATE getWebSocketStatus() {
    return READYSTATE.OPEN;
  }

  WebSocketBase getSocketBase() {
    return socketBase;
  }

  void setSocketBase(WebSocketBase socketBase) {
    this.socketBase = socketBase;
  }

  BlockingQueue<ExchangeEvent> getMarketDataEventQueue() {
    return marketDataEventQueue;
  }
}
