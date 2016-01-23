package com.xeiam.xchange.okcoin;

import static com.xeiam.xchange.currency.Currency.BTC;
import static com.xeiam.xchange.currency.Currency.LTC;
import static com.xeiam.xchange.currency.Currency.USD;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.xeiam.xchange.currency.Currency;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.account.AccountInfo;
import com.xeiam.xchange.dto.account.Balance;
import com.xeiam.xchange.dto.account.Wallet;
import com.xeiam.xchange.dto.marketdata.OrderBook;
import com.xeiam.xchange.dto.marketdata.Ticker;
import com.xeiam.xchange.dto.marketdata.Trade;
import com.xeiam.xchange.dto.marketdata.Trades;
import com.xeiam.xchange.dto.marketdata.Trades.TradeSortType;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.dto.trade.OpenOrders;
import com.xeiam.xchange.dto.trade.UserTrade;
import com.xeiam.xchange.dto.trade.UserTrades;
import com.xeiam.xchange.okcoin.dto.account.OkCoinFunds;
import com.xeiam.xchange.okcoin.dto.account.OkCoinFuturesInfoCross;
import com.xeiam.xchange.okcoin.dto.account.OkCoinFuturesUserInfoCross;
import com.xeiam.xchange.okcoin.dto.account.OkCoinUserInfo;
import com.xeiam.xchange.okcoin.dto.account.OkcoinFuturesFundsCross;
import com.xeiam.xchange.okcoin.dto.marketdata.OkCoinDepth;
import com.xeiam.xchange.okcoin.dto.marketdata.OkCoinTickerResponse;
import com.xeiam.xchange.okcoin.dto.marketdata.OkCoinTrade;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinFuturesOrder;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinFuturesOrderResult;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrder;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrderResult;

public final class OkCoinAdapters {

  private static final Balance zeroUsdBalance = new Balance(USD, BigDecimal.ZERO);
  public static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

  private OkCoinAdapters() {

  }


  public static String adaptSymbol(CurrencyPair currencyPair) {

    return currencyPair.base.getCurrencyCode().toLowerCase() + "_" + currencyPair.counter.getCurrencyCode().toLowerCase();
  }

  public static CurrencyPair adaptSymbol(String symbol) {

    String[] currencies = symbol.toUpperCase().split("_");
    return new CurrencyPair(currencies[0], currencies[1]);
  }

  public static Ticker adaptTicker(OkCoinTickerResponse tickerResponse, CurrencyPair currencyPair) {
    long date = tickerResponse.getDate();
    return new Ticker.Builder().currencyPair(currencyPair).high(tickerResponse.getTicker().getHigh()).low(tickerResponse.getTicker().getLow())
        .bid(tickerResponse.getTicker().getBuy()).ask(tickerResponse.getTicker().getSell()).last(tickerResponse.getTicker().getLast())
        .volume(tickerResponse.getTicker().getVol()).timestamp(new Date(date * 1000L)).build();
  }

  public static OrderBook adaptOrderBook(OkCoinDepth depth, CurrencyPair currencyPair) {

    List<LimitOrder> asks = adaptLimitOrders(OrderType.ASK, depth.getAsks(), currencyPair);
    Collections.reverse(asks);

    List<LimitOrder> bids = adaptLimitOrders(OrderType.BID, depth.getBids(), currencyPair);
    return new OrderBook(depth.getTimestamp(), asks, bids);
  }

  public static Trades adaptTrades(OkCoinTrade[] trades, CurrencyPair currencyPair) {

    List<Trade> tradeList = new ArrayList<Trade>(trades.length);
    for (int i = 0; i < trades.length; i++) {
      OkCoinTrade trade = trades[i];
      tradeList.add(adaptTrade(trade, currencyPair));
    }
    long lastTid = trades.length > 0 ? (trades[trades.length - 1].getTid()) : 0L;
    return new Trades(tradeList, lastTid, TradeSortType.SortByTimestamp);
  }

  public static AccountInfo adaptAccountInfo(OkCoinUserInfo userInfo) {

    OkCoinFunds funds = userInfo.getInfo().getFunds();

    Map<String, Balance.Builder> builders = new TreeMap<String, Balance.Builder>();

    for (Map.Entry<String,BigDecimal> available : funds.getFree().entrySet()) {
      builders.put(available.getKey(), new Balance.Builder().currency(Currency.getInstance(available.getKey())).available(available.getValue()));
    }

    for (Map.Entry<String,BigDecimal> frozen : funds.getFreezed().entrySet()) {
      Balance.Builder builder = builders.get(frozen.getKey());
      if (builder == null)
        builder = new Balance.Builder().currency(Currency.getInstance(frozen.getKey()));
      builders.put(frozen.getKey(), builder.frozen(frozen.getValue()));
    }

    for (Map.Entry<String,BigDecimal> borrowed : funds.getBorrow().entrySet()) {
      Balance.Builder builder = builders.get(borrowed.getKey());
      if (builder == null)
        builder = new Balance.Builder().currency(Currency.getInstance(borrowed.getKey()));
      builders.put(borrowed.getKey(), builder.borrowed(borrowed.getValue()));
    }

    List<Balance> wallet = new ArrayList<Balance>(builders.size());

    for (Balance.Builder builder : builders.values()) {
      wallet.add(builder.build());
    }

    return new AccountInfo(new Wallet(wallet));
  }

  public static AccountInfo adaptAccountInfoFutures(OkCoinFuturesUserInfoCross futureUserInfo) {
    OkCoinFuturesInfoCross info = futureUserInfo.getInfo();
    OkcoinFuturesFundsCross btcFunds = info.getBtcFunds();
    OkcoinFuturesFundsCross ltcFunds = info.getLtcFunds();

    Balance btcBalance = new Balance(BTC, btcFunds.getAccountRights());
    Balance ltcBalance = new Balance(LTC, ltcFunds.getAccountRights());

    return new AccountInfo(new Wallet(zeroUsdBalance, btcBalance, ltcBalance));
  }

  public static OpenOrders adaptOpenOrders(List<OkCoinOrderResult> orderResults) {
    List<LimitOrder> openOrders = new ArrayList<LimitOrder>();

    for (int i = 0; i < orderResults.size(); i++) {
      OkCoinOrderResult orderResult = orderResults.get(i);
      OkCoinOrder[] orders = orderResult.getOrders();
      for (int j = 0; j < orders.length; j++) {
        OkCoinOrder singleOrder = orders[j];
        openOrders.add(adaptOpenOrder(singleOrder));
      }
    }
    return new OpenOrders(openOrders);
  }

  public static OpenOrders adaptOpenOrdersFutures(List<OkCoinFuturesOrderResult> orderResults) {
    List<LimitOrder> openOrders = new ArrayList<LimitOrder>();

    for (int i = 0; i < orderResults.size(); i++) {
      OkCoinFuturesOrderResult orderResult = orderResults.get(i);
      OkCoinFuturesOrder[] orders = orderResult.getOrders();
      for (int j = 0; j < orders.length; j++) {
        OkCoinFuturesOrder singleOrder = orders[j];
        openOrders.add(adaptOpenOrderFutures(singleOrder));
      }
    }
    return new OpenOrders(openOrders);
  }

  public static UserTrades adaptTrades(OkCoinOrderResult orderResult) {

    List<UserTrade> trades = new ArrayList<UserTrade>(orderResult.getOrders().length);
    for (int i = 0; i < orderResult.getOrders().length; i++) {
      OkCoinOrder order = orderResult.getOrders()[i];

      // skip cancels that have not yet been filtered out
      if (order.getDealAmount().equals(BigDecimal.ZERO)) {
        continue;
      }
      trades.add(adaptTrade(order));
    }
    return new UserTrades(trades, TradeSortType.SortByTimestamp);
  }

  public static UserTrades adaptTradesFutures(OkCoinFuturesOrderResult orderResult) {

    List<UserTrade> trades = new ArrayList<UserTrade>(orderResult.getOrders().length);
    for (int i = 0; i < orderResult.getOrders().length; i++) {
      OkCoinFuturesOrder order = orderResult.getOrders()[i];

      // skip cancels that have not yet been filtered out
      if (order.getDealAmount().equals(BigDecimal.ZERO)) {
        continue;
      }
      trades.add(adaptTradeFutures(order));
    }
    return new UserTrades(trades, TradeSortType.SortByTimestamp);
  }

  private static List<LimitOrder> adaptLimitOrders(OrderType type, BigDecimal[][] list, CurrencyPair currencyPair) {

    List<LimitOrder> limitOrders = new ArrayList<LimitOrder>(list.length);
    for (int i = 0; i < list.length; i++) {
      BigDecimal[] data = list[i];
      limitOrders.add(adaptLimitOrder(type, data, currencyPair, null, null));
    }
    return limitOrders;
  }

  private static LimitOrder adaptLimitOrder(OrderType type, BigDecimal[] data, CurrencyPair currencyPair, String id, Date timestamp) {

    return new LimitOrder(type, data[1], currencyPair, id, timestamp, data[0]);
  }

  private static Trade adaptTrade(OkCoinTrade trade, CurrencyPair currencyPair) {

    return new Trade(trade.getType().equals("buy") ? OrderType.BID : OrderType.ASK, trade.getAmount(), currencyPair, trade.getPrice(),
        trade.getDate(), "" + trade.getTid());
  }

  private static LimitOrder adaptOpenOrder(OkCoinOrder order) {

    return new LimitOrder(adaptOrderType(order.getType()), order.getAmount().subtract(order.getDealAmount()), adaptSymbol(order.getSymbol()),
        String.valueOf(order.getOrderId()), order.getCreateDate(), order.getPrice());
  }

  private static LimitOrder adaptOpenOrderFutures(OkCoinFuturesOrder order) {
    return new LimitOrder(adaptOrderType(order.getType()), order.getAmount().subtract(order.getDealAmount()), adaptSymbol(order.getSymbol()),
        String.valueOf(order.getOrderId()), order.getCreatedDate(), order.getPrice());
  }

  public static OrderType adaptOrderType(String type) {

    return type.equals("buy") || type.equals("buy_market") || type.equals("1") || type.equals("4") ? OrderType.BID : OrderType.ASK;
  }

  private static UserTrade adaptTrade(OkCoinOrder order) {

    return new UserTrade(adaptOrderType(order.getType()), order.getDealAmount(), adaptSymbol(order.getSymbol()), order.getPrice(),
        order.getCreateDate(), null, String.valueOf(order.getOrderId()), null, (Currency)null);
  }

  private static UserTrade adaptTradeFutures(OkCoinFuturesOrder order) {

    return new UserTrade(adaptOrderType(order.getType()), order.getDealAmount(), adaptSymbol(order.getSymbol()), order.getPrice(),
        order.getCreatedDate(), null, String.valueOf(order.getOrderId()), null, (Currency)null);
  }

  public static Trade adaptStreamingTrade(JsonNode trade, CurrencyPair currencyPair) {
    String id = trade.get(0).asText();
    double price = trade.get(1).asDouble();
    double amount = trade.get(2).asDouble();
    String dateString = (trade.get(3).asText());
    OrderType orderType = trade.get(4).asText().equals("bid") ? OrderType.BID : OrderType.ASK;
  
    Date tradeDate = null;
    try {
  
      tradeDate = OkCoinAdapters.dateFormat.parse(dateString);
      //TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  
      // dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Hong Kong"));
      // dateFormat.setTimeZone("Asia/Hong Kong");
  
      tradeDate.setTime(OkCoinUtils.toEpoch(tradeDate, "VST"));
    } catch (ParseException e) {
      tradeDate = null;
    }
  
    return new Trade(orderType, BigDecimal.valueOf(amount), currencyPair, BigDecimal.valueOf(price), tradeDate, id);
  }
}
