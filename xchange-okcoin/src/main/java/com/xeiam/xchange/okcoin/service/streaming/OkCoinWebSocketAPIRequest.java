package com.xeiam.xchange.okcoin.service.streaming;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

abstract class OkCoinWebSocketAPIRequest {

  
  protected Map<String, String> params;
  protected Object result = null;
  protected ChannelProvider channelProvider;

  private CountDownLatch latch = new CountDownLatch(1);
  private boolean IOException;

  abstract String getChannel();
  
  abstract RequestIdentifier getIdentifier();
  
  Map<String, String> getParams() {
    return params;
  }

  void set(Object result) {
    
    this.result = result;
    this.IOException = false;
    latch.countDown();
  }
  OkCoinWebSocketAPIRequest(ChannelProvider channelProvider) {
    
    this.channelProvider = channelProvider;
  }

  public boolean cancel(boolean mayInterruptIfRunning) {
    
    return false;
  }

  public Object get() throws InterruptedException, ExecutionException {
    
    latch.await();
    if (IOException)
      throw new ExecutionException(new java.io.IOException("Could not deliver message or receive response."));
    return result;
  }

  public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
    
    latch.await(timeout, unit);
    if (IOException)
      throw new ExecutionException(new java.io.IOException("Could not deliver message or receive response."));
    return result;
  }

  public boolean isCancelled() {
    
    return false;
  }

  public boolean isDone() {
    
    return latch.getCount() == 0;
  }
  
  public void setIOException() {
    
    IOException = true;
    latch.countDown();
  }

  @Override
  public String toString() {
    return "OkCoinWebSocketAPIRequest [getIdentifier()=" + getIdentifier() + "]";
  }

}
