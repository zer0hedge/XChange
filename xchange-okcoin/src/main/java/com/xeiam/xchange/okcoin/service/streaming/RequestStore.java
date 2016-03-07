package com.xeiam.xchange.okcoin.service.streaming;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xeiam.xchange.service.streaming.ExchangeEvent;

class RequestStore {
  
  
  void processResponse(ExchangeEvent event, RequestIdentifier id, Object result) throws InterruptedException {
    OkCoinWebSocketAPIRequest req = take(id);
    if (req != null)
      req.set(result);
    else
      log.error("Unexpected {} event: {}", event.getEventType(), event);
  }
  

  synchronized void put(OkCoinWebSocketAPIRequest request) throws InterruptedException {
    
    if (requests.get(request.getIdentifier()) != null)
      requests.get(request.getIdentifier()).put(request);
    else {
      requests.put(request.getIdentifier(), new ArrayBlockingQueue<OkCoinWebSocketAPIRequest>(10));
      requests.get(request.getIdentifier()).put(request);
    }
  }

  synchronized OkCoinWebSocketAPIRequest take(RequestIdentifier id) throws InterruptedException {
    
    if (requests.get(id) == null)
      return null;
    
    OkCoinWebSocketAPIRequest request = requests.get(id).take();
    if (requests.get(id).size() == 0)
      requests.remove(id);
    return request;
  }
  
  synchronized void broadcastDisconnection() throws InterruptedException {
    
    for (RequestIdentifier id : requests.keySet()) {
      while (requests.get(id).size()>0)
       requests.get(id).take().setIOException();
      requests.remove(id);
    }
  }

  private ConcurrentMap<RequestIdentifier, ArrayBlockingQueue<OkCoinWebSocketAPIRequest>> requests = new ConcurrentHashMap<>();

  private Logger log = LoggerFactory.getLogger(this.getClass());
}
