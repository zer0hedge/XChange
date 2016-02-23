package com.xeiam.xchange.okcoin.service.streaming;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class RequestStore {

  synchronized void put(OkCoinWebSocketAPIRequest request) throws InterruptedException {
    
    if (requests.get(request.getIdentifier()) != null)
      requests.get(request.getIdentifier()).put(request);
    else {
      requests.put(request.getIdentifier(), new ArrayBlockingQueue<OkCoinWebSocketAPIRequest>(10));
      requests.get(request.getIdentifier()).put(request);
    }
  }

  synchronized OkCoinWebSocketAPIRequest take(RequestIdentifier id) throws InterruptedException {

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

}