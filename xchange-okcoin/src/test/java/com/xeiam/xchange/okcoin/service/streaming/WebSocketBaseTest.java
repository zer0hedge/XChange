package com.xeiam.xchange.okcoin.service.streaming;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import com.xeiam.xchange.okcoin.OkCoinDigest;

import io.netty.channel.Channel;

public class WebSocketBaseTest {

  @Test
  public void testAddChannelWithParameters() {

    WebSocketBase sut = new WebSocketBase("", null);
    sut.channel = mock(Channel.class);
    when(sut.channel.writeAndFlush(anyObject())).thenReturn(null);
    sut.setStatus(true);
    
    OkCoinDigest signatureCreator = new OkCoinDigest("123456", "lol");

    Map<String, String> params = new HashMap<>();
    params.put("price", "100500");
    params.put("api_key", "123456");
    String sign = signatureCreator.digestNameValueParamMap(new ArrayList<>(params.entrySet()));
    params.put("sign", sign);

    WebSocketBase spy = Mockito.spy(sut);
    spy.addOneTimeChannel("ok_spotusd_trade", params);
    verify(spy).sendMessage("{'event':'addChannel','channel':'ok_spotusd_trade'," + "'parameters':"
        + "{'api_key':'123456'," + "'price':'100500'," + "'sign':'F6DCE90FF040A5A8327AD70936D93EF9'" + "}}");

  }

  WebSocketBase sut;
}
