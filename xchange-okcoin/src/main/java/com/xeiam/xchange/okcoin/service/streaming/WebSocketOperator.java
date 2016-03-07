package com.xeiam.xchange.okcoin.service.streaming;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;

class WebSocketOperator {
  private static final Logger log = LoggerFactory.getLogger(WebSocketOperator.class);

  private OkCoinEventParser service = null;
  private Timer timerTask = null;
  private ConnectionMonitor monitor = null;
  private EventLoopGroup group = null;
  private Bootstrap bootstrap = null;
  Channel channel = null;
  private String url = null;
  private ChannelFuture future = null;
  private volatile boolean isAlive = false;

  Set<String> subscribedChannels = new HashSet<String>();

  private String name = "";

  WebSocketOperator(String url, OkCoinEventParser service) {
    this.url = url;
    this.service = service;
  }

  void start() {
    if (url == null) {
      log.info("WebSocketClient start error  url can not be null");
      return;
    }
    if (service == null) {
      log.info("WebSocketClient start error  WebSocketService can not be null");
      return;
    }

    monitor = new ConnectionMonitor(this);
    monitor.setName(name + "monitor");

    while (!Thread.interrupted())
      try {
        this.connect();
        break;
      } catch (TimeoutException e) {
        log.warn(e.getMessage());
        continue;
      }

    timerTask = new Timer();
    timerTask.schedule(monitor, 3000, 1000);
  }

  void setAlive(boolean flag) {
    this.isAlive = flag;
  }

  void addChannel(String channel) {
    if (channel == null) {
      return;
    }
    String dataMsg = "{'event':'addChannel','channel':'" + channel + "'}";
    this.sendMessage(dataMsg);
    subscribedChannels.add(channel);
  }

  void addOneTimeChannel(String channel, Map<String, String> params) {
    if (channel == null) {
      return;
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    try {
      String paramString = mapper.writeValueAsString(params).replace("\"", "'");
      String dataMsg = "{'event':'addChannel','channel':'" + channel + "','parameters':" + paramString + "}";
      sendMessage(dataMsg);
    } catch (JsonProcessingException e) {
      log.warn("Bad parameters passed" + e.getMessage());
    }
  }

  void removeChannel(String channel) {
    if (channel == null) {
      return;
    }
    String dataMsg = "{'event':'removeChannel','channel':'" + channel + "'}";
    this.sendMessage(dataMsg);
    subscribedChannels.remove(channel);
  }

  void connect() throws TimeoutException {
    try {

      group = new NioEventLoopGroup(1);
      bootstrap = new Bootstrap();
      final SslContext sslCtx = SslContext.newClientContext();
      final URI uri = new URI(url);
      final WebSocketHandler handler = new WebSocketHandler(this, WebSocketClientHandshakerFactory.newHandshaker(uri,
          WebSocketVersion.V13, null, false, new DefaultHttpHeaders(), Integer.MAX_VALUE), service, monitor);

      bootstrap.group(group).option(ChannelOption.TCP_NODELAY, true).channel(NioSocketChannel.class)
          .handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) {
              ChannelPipeline p = ch.pipeline();
              if (sslCtx != null) {
                p.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));
              }
              p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192), handler);
            }
          });

      future = bootstrap.connect(uri.getHost(), uri.getPort());

      log.debug("Waiting for channel future to finish...");
      if (future.await(20000)) {
        channel = future.sync().channel();
        log.debug("Waiting for handshake...");
        if (handler.handshakeFuture().await(20000)) {
          Thread.sleep(1000);
          this.setAlive(true);
        }
        else
          throw new TimeoutException("Handshake timed out");
      } else
        throw new TimeoutException("Connection timed out");

    } catch (URISyntaxException | SSLException | InterruptedException e) {
      log.warn(e.getMessage());
    }
  }

  void sendMessage(String message) {
    try {
      while (!isAlive) {
        Thread.sleep(100);
      }
      
      log.debug("Sending message: " + message);
      channel.writeAndFlush(new TextWebSocketFrame(message));

    } catch (InterruptedException e) {

    }
  }

  void sendPing() {
    if (!isAlive)
      throw new RejectedExecutionException("WebSocket is not alive!");
    String message = "{'event':'ping'}";
    log.debug("Sending ping: " + message);
    channel.writeAndFlush(new TextWebSocketFrame(message));
  }

  void shutdown() {
    log.debug("Shutting down...");
    group.shutdownGracefully();
  }

  void reconnect() {
    while (!Thread.interrupted())
      try {
        log.debug("Reconnecting");
        shutdown();
        connect();

        if (future.isSuccess()) {

          Iterator<String> it = subscribedChannels.iterator();
          while (it.hasNext()) {
            String channel = it.next();
            addChannel(channel);
          }
          return;
        }
      } catch (TimeoutException e) {
        log.warn(e.getMessage());
        continue;
      }
  }

  public void setName(String name) {
    this.name = name;
  }
}