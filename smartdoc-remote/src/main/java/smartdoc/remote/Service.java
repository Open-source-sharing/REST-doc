package smartdoc.remote;

import java.util.Map;

public interface Service {

  Status status();

  void start() throws ServiceException;

  void stop() throws ServiceException;

  Map<String, Handler> getHandlerTable();
}
