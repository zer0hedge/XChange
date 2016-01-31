package com.xeiam.xchange.okcoin.service.streaming;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

abstract class OkCoinWebSocketAPIRequest {
	
	protected Map<String, String> params;
	protected Object result = null;
	protected ChannelProvider channelProvider;
	
	private CountDownLatch latch = new CountDownLatch(1);
	
	abstract String getChannel();
	abstract Long getOrderId();


	Map<String, String> getParams() {
		return params;
	}
	
	void set(Object result) {
		this.result = result;
		latch.countDown();
		
	}
	
	OkCoinWebSocketAPIRequest(ChannelProvider channelProvider) {
		this.channelProvider = channelProvider;
		
	}
	

	public boolean cancel(boolean mayInterruptIfRunning) {
		// TODO Auto-generated method stub
		return false;
	}

	public Object get() throws InterruptedException {
		latch.await();
		return result;
	}

	public Object get(long timeout, TimeUnit unit) throws InterruptedException {
		latch.await(timeout, unit);
		return result;
	}

	public boolean isCancelled() {
		return false;
	}

	public boolean isDone() {
		return latch.getCount() == 0;
	}

}
