package com.xeiam.xchange.okcoin.service.streaming;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.okcoin.OkCoinDigest;

public class OkCoinCancelOrderRequest extends OkCoinWebSocketAPIRequest implements Future<Boolean> {

	private String orderId;

	OkCoinCancelOrderRequest(String orderId, String symbol, ChannelProvider channelProvider,  String apikey, OkCoinDigest signatureCreator) {
		super(channelProvider);
		this.orderId = orderId;
		params = new HashMap<String, String>();
		params.put("api_key", apikey);
		params.put("symbol", symbol);
		params.put("order_id", orderId);
		String sign = signatureCreator.digestNameValueParamMap(new ArrayList<>(params.entrySet()));
		params.put("sign", sign);
	}

	Long getOrderId() {
		return Long.valueOf(orderId);
	}

	@Override
	String getChannel() {
		return channelProvider.getCancelOrder();
	}
	
	public Boolean get() throws InterruptedException{
		return checkResult((Boolean) super.get());
	}
	
	public Boolean get(long timeout, TimeUnit unit) throws InterruptedException {
		return checkResult((Boolean) super.get(timeout,unit));
	}

	Boolean checkResult(Boolean result) {
		if(!result)
			throw new ExchangeException("Unable to perform request");
		return result;
	}


}
