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

class OkCoinBaseStreamingService implements StreamingExchangeService {
  private WebSocketOperator socketBase;
  protected final ChannelProvider channelProvider;
  private final BlockingQueue<ExchangeEvent> eventQueue = new LinkedBlockingQueue<ExchangeEvent>();

  private final OkCoinStreamingConfiguration exchangeStreamingConfiguration;

  OkCoinBaseStreamingService(Exchange exchange, ExchangeStreamingConfiguration streamingConfiguration, String name) {
    
    this.exchangeStreamingConfiguration = (OkCoinStreamingConfiguration) streamingConfiguration;

    ExchangeSpecification exchangeSpecification = exchange.getExchangeSpecification();
    String sslUri = (String) exchangeSpecification.getExchangeSpecificParametersItem("Websocket_SslUri");
    boolean useFutures = (Boolean) exchangeSpecification.getExchangeSpecificParametersItem("Use_Futures");

    channelProvider = useFutures ? new FuturesChannelProvider(OkCoinExchange.futuresContractOfConfig(exchangeSpecification))
        : new SpotChannelProvider(exchangeSpecification);
    OkCoinEventParser socketService = new OkCoinEventParser(eventQueue,
        channelProvider,
        this.exchangeStreamingConfiguration.getMarketDataCurrencyPairs());
    
    socketBase = new WebSocketOperator(sslUri, socketService);
    socketBase.setName(name);
  }

  @Override
  public void connect() {
    socketBase.start();
  }

  @Override
  public void disconnect() {
	  socketBase.shutdown();
  }

  @Override
  public ExchangeEvent getNextEvent() throws InterruptedException {
    return eventQueue.take();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int countEventsAvailable() {
    return eventQueue.size();
  }

  @Override
  public void send(String msg) {
  }

  @Override
  public READYSTATE getWebSocketStatus() {
    return READYSTATE.OPEN;
  }

  WebSocketOperator getSocketBase() {
    return socketBase;
  }

  void setSocketBase(WebSocketOperator socketBase) {
    this.socketBase = socketBase;
  }

  BlockingQueue<ExchangeEvent> getMarketDataEventQueue() {
    return eventQueue;
  }
}
