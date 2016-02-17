package com.xeiam.xchange.okcoin.service.streaming;

import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFuture;

class MonitorTask extends TimerTask {
	private long lastResponseTime = System.currentTimeMillis();
	private final int checkTime = 5000;
	private WebSocketBase client = null;
	private String name = "";
	private Logger log = LoggerFactory.getLogger(this.getClass());

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
		} else {
			ChannelFuture future = client.sendPing();
			try {
				future.await();
				if (!future.isSuccess() && future.cause() != null) {
					log.debug("Ping send failed, reconnecting ... ", future.cause());
					this.client.reConnect();
				} else
					log.debug("Ping sent");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			catch (RejectedExecutionException e) {
				this.client.reConnect();
			}
		}
	}
}