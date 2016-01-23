package com.xeiam.xchange.okcoin.service.streaming;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface WebSocketService {
  public void onReceive(String msg) throws JsonParseException, JsonMappingException, IOException;

  public void onDisconnect();
}