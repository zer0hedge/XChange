package com.xeiam.xchange.okcoin.service.streaming;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

interface WebSocketService {
  void onReceive(String msg) throws JsonParseException, JsonMappingException, IOException;

  void onDisconnect();
}