package com.xeiam.xchange.okcoin.service.streaming;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.okcoin.OkCoinDigest;
import com.xeiam.xchange.service.streaming.ExchangeEventType;

public class OkCoinCancelOrderRequest extends OkCoinWebSocketAPIRequest implements Future<Boolean> {


	OkCoinCancelOrderRequest(String orderId, String symbol, ChannelProvider channelProvider,  String apikey, OkCoinDigest signatureCreator) {
		super(channelProvider);
		params = new HashMap<String, String>();
		params.put("api_key", apikey);
		params.put("symbol", symbol);
		params.put("order_id", orderId);
		String sign = signatureCreator.digestNameValueParamMap(new ArrayList<>(params.entrySet()));
		params.put("sign", sign);
	}

	
	/*
	 * Can't use orderId as identifier as error responses do not provide orderId
	 */
	@Override
	RequestIdentifier getIdentifier() {
		return new RequestIdentifier(null, ExchangeEventType.ORDER_CANCELED);
	}

	@Override
	String getChannel() {
		return channelProvider.getCancelOrder();
	}
	
	public Boolean get() throws InterruptedException, ExecutionException {
		return checkResult((Boolean) super.get());
	}
	
	public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
		return checkResult((Boolean) super.get(timeout,unit));
	}

	Boolean checkResult(Boolean result) {
		if(!result)
			throw new ExchangeException("Could not cancel order");
		return result;
	}



}
