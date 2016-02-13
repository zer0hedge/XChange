package com.xeiam.xchange.okcoin.service.streaming;

import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;

class MonitorTask extends TimerTask {
  private long lastResponseTime = System.currentTimeMillis();
  private final int checkTime = 5000;
  private WebSocketBase client = null;
  private String name = "";

  void updateTime() {
    lastResponseTime = System.currentTimeMillis();
  }

  MonitorTask(WebSocketBase client) {
    this.client = client;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void run() {
    if (!name.equals(""))
      Thread.currentThread().setName(name);
    
    if (System.currentTimeMillis() - lastResponseTime > checkTime) {
      client.reConnect();
    }
    try {
      client.sendPing();
    } catch (RejectedExecutionException reject) {
      client.reConnect();
    }
  }
}