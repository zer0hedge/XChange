package com.xeiam.xchange.okcoin;

import java.math.BigDecimal;

import org.junit.Test;

import com.xeiam.xchange.dto.Order.DefaultOrderFlags;
import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.okcoin.dto.trade.OkCoinOrder;

import static org.junit.Assert.*;

public class OkCoinAdaptersTest {

  @Test
  public void testAdaptOrder() {
    
    OkCoinOrder serverOrder = new OkCoinOrder(100L, -1, "btc_usd", "", BigDecimal.TEN,
        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN.divide(new BigDecimal("2")),
        null);
    
    LimitOrder adaptedOrder = OkCoinAdapters.adaptOrder(serverOrder);
    
    assertTrue(adaptedOrder.getOrderFlags().contains(DefaultOrderFlags.CANCELLED));
    assertEquals(BigDecimal.TEN.divide(new BigDecimal("2")), adaptedOrder.getTradableAmount());
    
    serverOrder = new OkCoinOrder(100L, 2, "btc_usd", "", BigDecimal.TEN,
        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN.divide(new BigDecimal("2")),
        null);
    
    adaptedOrder = OkCoinAdapters.adaptOrder(serverOrder);
    
    assertTrue(adaptedOrder.getOrderFlags().contains(null));
    assertEquals(BigDecimal.TEN.divide(new BigDecimal("2")), adaptedOrder.getTradableAmount());
  }
  
}
