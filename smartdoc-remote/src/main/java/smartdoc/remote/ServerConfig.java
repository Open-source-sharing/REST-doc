package smartdoc.remote;

public interface ServerConfig extends ServiceConfig {

  default boolean useEpoll() {
    return false;
  }

  default int getServerSelectorThreads() {
    return 1;
  }

  int getServerSocketSndBufSize();

  int getServerSocketRcvBufSize();

  int getServerWorkerThreads();
}
