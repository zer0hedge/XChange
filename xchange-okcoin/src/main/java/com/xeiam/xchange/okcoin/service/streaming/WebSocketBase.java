package com.xeiam.xchange.okcoin.service.streaming;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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

public class WebSocketBase {
  private static final Logger log = LoggerFactory.getLogger(WebSocketBase.class);

  private WebSocketService service = null;
  private Timer timerTask = null;
  private MonitorTask monitor = null;
  private EventLoopGroup group = null;
  private Bootstrap bootstrap = null;
  Channel channel = null;
  private String url = null;
  private ChannelFuture future = null;
  private boolean isAlive = false;
  Set<String> subscribedChannels = new HashSet<String>();

  public WebSocketBase(String url, WebSocketService service) {
    this.url = url;
    this.service = service;
  }

  public void start() {
    if (url == null) {
      log.info("WebSocketClient start error  url can not be null");
      return;
    }
    if (service == null) {
      log.info("WebSocketClient start error  WebSocketService can not be null");
      return;
    }

    monitor = new MonitorTask(this);
    this.connect();

    timerTask = new Timer();
    timerTask.schedule(monitor, 1000, 3000);
  }

  public void setStatus(boolean flag) {
    this.isAlive = flag;
  }

  public void addChannel(String channel) {
    if (channel == null) {
      return;
    }
    String dataMsg = "{'event':'addChannel','channel':'" + channel + "'}";
    this.sendMessage(dataMsg);
    subscribedChannels.add(channel);
  }

  public void addOneTimeChannel(String channel, Map<String, String> params) {
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

  public void removeChannel(String channel) {
    if (channel == null) {
      return;
    }
    String dataMsg = "{'event':'removeChannel','channel':'" + channel + "'}";
    this.sendMessage(dataMsg);
    subscribedChannels.remove(channel);
  }

  private void connect() {
    try {
      final URI uri = new URI(url);

      group = new NioEventLoopGroup(1);
      bootstrap = new Bootstrap();
      final SslContext sslCtx = SslContext.newClientContext();
      final WebSocketClientHandler handler = new WebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(
          uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders(), Integer.MAX_VALUE), service, monitor);

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
      future.addListener(new ChannelFutureListener() {
        public void operationComplete(final ChannelFuture future) throws Exception {
        }
      });
      channel = future.sync().channel();
      handler.handshakeFuture().sync();
      this.setStatus(true);

    } catch (Exception e) {
      log.info("WebSocketClient start error ", e);
      group.shutdownGracefully();
      this.setStatus(false);
    }
  }

  public void sendMessage(String message) {
    if (!isAlive) {
      log.info("WebSocket is not Alive addChannel error");
    }
    log.debug("Sending message: "+message);
    channel.writeAndFlush(new TextWebSocketFrame(message));
  }

  public void sendPing() {
    String dataMsg = "{'event':'ping'}";
    this.sendMessage(dataMsg);
  }

  public void reConnect() {
    try {
      log.debug("Reconnecting");
      this.group.shutdownGracefully();
      this.group = null;
      this.connect();

      if (future.isSuccess()) {
        this.setStatus(true);
        this.sendPing();
        Iterator<String> it = subscribedChannels.iterator();
        while (it.hasNext()) {
          String channel = it.next();
          this.addChannel(channel);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}