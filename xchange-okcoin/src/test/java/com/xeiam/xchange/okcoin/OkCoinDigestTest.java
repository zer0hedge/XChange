package com.xeiam.xchange.okcoin;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.junit.Test;

import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.dto.Order.OrderType;
import com.xeiam.xchange.dto.trade.LimitOrder;

public class OkCoinDigestTest {

  @Test
  public void testSignatureGeneration() {
    Map<String, String> dummy_entries = new HashMap<>();
    dummy_entries.put("api_key", "123456");
    dummy_entries.put("price", "100500");

    List<Entry<String, String>> entriesList = new ArrayList<>(dummy_entries.entrySet());
    String sign = new OkCoinDigest("123456", "lol").digestNameValueParamMap(entriesList);

    assertEquals("F6DCE90FF040A5A8327AD70936D93EF9", sign);

  }

  @Test
  public void testSignatureGeneration2() {

    LimitOrder limitOrder = new LimitOrder(OrderType.ASK, new BigDecimal("0.21"), CurrencyPair.BTC_USD, "", null,
        new BigDecimal("395.88"));

    Map<String, String> params = new HashMap<>();
    params.put("api_key", "123456");
    params.put("symbol", limitOrder.getCurrencyPair().toString().replace("/", "_").toLowerCase());
    params.put("type", limitOrder.getType() == OrderType.ASK ? "sell" : "buy");
    params.put("price", limitOrder.getLimitPrice().toPlainString());
    params.put("amount", limitOrder.getTradableAmount().toPlainString());
    String sign = new OkCoinDigest("123456", "lol").digestNameValueParamMap(new ArrayList<>(params.entrySet()));

    assertEquals("7A432CBCD6598C6067E59C15A40FCA35", sign);

  }

  @Test
  public void concurrentSignatureGenerationTest() throws InterruptedException, BrokenBarrierException {
    LimitOrder limitOrder = new LimitOrder(OrderType.ASK, new BigDecimal("0.21"), CurrencyPair.BTC_USD, "", null,
        new BigDecimal("397.65"));

    final Map<String, String> params = new HashMap<>();
    params.put("api_key", "123456");
    params.put("symbol", limitOrder.getCurrencyPair().toString().replace("/", "_").toLowerCase());
    params.put("type", limitOrder.getType() == OrderType.ASK ? "sell" : "buy");
    params.put("price", limitOrder.getLimitPrice().toPlainString());
    params.put("amount", limitOrder.getTradableAmount().toPlainString());

    for (int i = 0; i < 500; i++) {

      Thread t1 = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            barrier.await();
            sign1 = new OkCoinDigest("123456", "lolol").digestNameValueParamMap(new ArrayList<>(params.entrySet()));
            barrier.await();
          } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
          }
        }
      });
      Thread t2 = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            barrier.await();
            sign2 = new OkCoinDigest("123456", "lol").digestNameValueParamMap(new ArrayList<>(params.entrySet()));
            barrier.await();
          } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
          }
        }
      });
      t1.start();
      t2.start();

      barrier.await();
      barrier.await();
      assertEquals("0C71EBC77DCDF86E23C394832105474E", sign1);
      assertEquals("0F98EFE9972A145EDED22B1612BD3D8D", sign2);
    }
  }

  private String sign1;
  private String sign2;
  private CyclicBarrier barrier = new CyclicBarrier(3);

}
