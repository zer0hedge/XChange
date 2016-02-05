package com.xeiam.xchange.okcoin.service.streaming;

import com.xeiam.xchange.service.streaming.ExchangeEventType;

class RequestIdentifier {

  public RequestIdentifier(Long id, ExchangeEventType responseEventType) {
    this.id = id;
    this.responseEventType = responseEventType;
  }

  private Long id;
  private ExchangeEventType responseEventType;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((responseEventType == null) ? 0 : responseEventType.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof RequestIdentifier))
      return false;
    RequestIdentifier other = (RequestIdentifier) obj;
    if (responseEventType != other.responseEventType)
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "RequestIdentifier [id=" + id + ", responseEventType=" + responseEventType + "]";
  }

}
