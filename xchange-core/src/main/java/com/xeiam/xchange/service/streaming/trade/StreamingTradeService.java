package com.xeiam.xchange.service.streaming.trade;

import java.io.IOException;

import com.xeiam.xchange.dto.trade.LimitOrder;
import com.xeiam.xchange.exceptions.ExchangeException;
import com.xeiam.xchange.exceptions.NotAvailableFromExchangeException;
import com.xeiam.xchange.exceptions.NotYetImplementedForExchangeException;
import com.xeiam.xchange.service.streaming.StreamingExchangeService;

/**
 * WebSocket/other-streaming-protocol based TradeService. Every method in the
 * implementation should be blocking and also synchronized to allow
 * multithreading.
 * 
 * In contrast to the StreamingMarketDataService the implementation cannot be
 * event driven because the methods will return/throw with respect to an event
 * received right after the request was sent. This is same protocol as for the
 * PollingServices however responds will arrive much sooner.
 */

// TODO: To develop a fully asynchronous model it may be convenient to create
// and AsynchronousStreamingTradeService with fully asynch methods.

public interface StreamingTradeService extends StreamingExchangeService {

  /**
   * Place a limit order
   *
   * @param limitOrder
   * @return the order ID
   * @throws ExchangeException
   *           - Indication that the exchange reported some kind of error with
   *           the request or response
   * @throws NotAvailableFromExchangeException
   *           - Indication that the exchange does not support the requested
   *           function or data
   * @throws NotYetImplementedForExchangeException
   *           - Indication that the exchange supports the requested function or
   *           data, but it has not yet been implemented
   * @throws IOException
   *           - Indication that a networking error occurred while fetching JSON
   *           data
   */
  public String placeLimitOrder(LimitOrder limitOrder)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException;

  /**
   * cancels order with matching orderId
   *
   * @param orderId
   * @return true if order was successfully cancelled, false otherwise.
   * @throws ExchangeException
   *           - Indication that the exchange reported some kind of error with
   *           the request or response
   * @throws NotAvailableFromExchangeException
   *           - Indication that the exchange does not support the requested
   *           function or data
   * @throws NotYetImplementedForExchangeException
   *           - Indication that the exchange supports the requested function or
   *           data, but it has not yet been implemented
   * @throws IOException
   *           - Indication that a networking error occurred while fetching JSON
   *           data
   */
  public void cancelOrder(String orderId)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException;;

  /**
   * Gets the order with matching orderId representing the corresponding order
   * from the exchange
   *
   * @param orderId
   * @throws ExchangeException
   *           - Indication that the exchange reported some kind of error with
   *           the request or response
   * @throws NotAvailableFromExchangeException
   *           - Indication that the exchange does not support the requested
   *           function or data
   * @throws NotYetImplementedForExchangeException
   *           - Indication that the exchange supports the requested function or
   *           data, but it has not yet been implemented
   * @throws IOException
   *           - Indication that a networking error occurred while fetching JSON
   *           data
   */
  public LimitOrder getOrder(String orderId)
      throws ExchangeException, NotAvailableFromExchangeException, NotYetImplementedForExchangeException, IOException;;

}
