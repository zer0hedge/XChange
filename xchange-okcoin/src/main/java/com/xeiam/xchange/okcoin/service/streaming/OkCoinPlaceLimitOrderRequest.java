package com.xeiam.xchange.okcoin.service.streaming;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.okcoin.OkCoinDigest;

public class OkCoinPlaceLimitOrderRequest extends OkCoinWebSocketAPIRequest implements Future<String> {

	private LimitOrder limitOrder;

	OkCoinPlaceLimitOrderRequest(LimitOrder limitOrder, ChannelProvider channelProvider,  String apikey, OkCoinDigest signatureCreator) {
		super(channelProvider);
		this.limitOrder = limitOrder;
		params = new HashMap<String, String>();
		params.put("api_key", apikey);
		params.put("symbol", limitOrder.getCurrencyPair().toString().replace("/", "_").toLowerCase());
		params.put("type", limitOrder.getType() == OrderType.ASK ? "sell" : "buy");
		params.put("price", limitOrder.getLimitPrice().toPlainString());
		params.put("amount", limitOrder.getTradableAmount().toPlainString());
		String sign = signatureCreator.digestNameValueParamMap(new ArrayList<>(params.entrySet()));
		params.put("sign", sign);
	}

	@Override
	String getChannel() {
		return channelProvider.getPlaceLimitOrder();
	}
	

	
	public String get() throws InterruptedException {
		return checkOrderId((String) super.get());
	}

	
	public String get(long timeout, TimeUnit unit) throws InterruptedException {
		return checkOrderId((String) super.get(timeout,unit));

	}



	public LimitOrder getOrder() {
		return limitOrder;
	}

	@Override
	Long getOrderId() {
		throw new UnsupportedOperationException("Order Id does not exists for this object!");
	}

	String checkOrderId(String orderId) {
		if(orderId.equals("-1"))
			throw new ExchangeException("Unable to perform request");
		return orderId;
	}

}
