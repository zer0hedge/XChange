package com.xeiam.xchange.okcoin.service.streaming;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.okcoin.OkCoinDigest;

public class OkCoinGetOrderInfoRequest extends OkCoinWebSocketAPIRequest implements Future<LimitOrder> {

	private String orderId;

	OkCoinGetOrderInfoRequest(String orderId, String symbol, ChannelProvider channelProvider,  String apikey, OkCoinDigest signatureCreator) {
		super(channelProvider);
		this.orderId = orderId;
		params = new HashMap<String, String>();
		params.put("api_key", apikey);
		params.put("symbol", symbol);
		params.put("order_id", orderId);
		String sign = signatureCreator.digestNameValueParamMap(new ArrayList<>(params.entrySet()));
		params.put("sign", sign);
	}

	Long getId() {
		return Long.valueOf(orderId);
	}

	@Override
	String getChannel() {
		return channelProvider.getOrderInfo();
	}
	
	public LimitOrder get() throws InterruptedException {
		return checkLimitOrder((LimitOrder) super.get());
	}
	
	public LimitOrder get(long timeout, TimeUnit unit) throws InterruptedException {
		return checkLimitOrder((LimitOrder) super.get(timeout,unit));
	}

	LimitOrder checkLimitOrder(LimitOrder result) {
		if(result == null)
			throw new ExchangeException("Unable to perform request");
		return result;
	}


}
