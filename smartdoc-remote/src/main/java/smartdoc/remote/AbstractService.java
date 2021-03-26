package smartdoc.remote;

import io.netty.handler.ssl.SslContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractService implements Service {

  Map<String, Handler> handlerTable = new ConcurrentHashMap<>();

  SslContext sslContext;

  Status status = Status.Stopped;

  @Override
  public Map<String, Handler> getHandlerTable() {
    return new HashMap<>(handlerTable);
  }

  public Status getStatus() {
    return status;
  }
}
