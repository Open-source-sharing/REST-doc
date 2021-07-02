package restdoc.client.api;

import restdoc.client.api.exception.DiffVersionException;
import restdoc.remoting.exception.*;
import restdoc.remoting.netty.NettyRemotingClient;
import restdoc.remoting.netty.NettyRequestProcessor;

import java.util.NoSuchElementException;

public interface Agent {

  NettyRemotingClient getRemotingClient();

  void start() throws RemotingException;

  Status getClientStatus();

  String getServerRemoteAddress();

  /**
   * @return true/false
   * @throws DiffVersionException if console and client version not match,will be throws
   */
  Boolean acknowledgeVersion()
      throws DiffVersionException, InterruptedException, RemotingConnectException,
          RemotingTimeoutException, RemotingTooMuchRequestException, RemotingSendRequestException;

  InvokeResult invoke(String taskId)
      throws NoSuchElementException, InterruptedException, RemotingTooMuchRequestException,
          RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException;

  InvokeResult invoke(RemotingTask remotingTask)
      throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException,
          RemotingConnectException, RemotingTooMuchRequestException;

  void addTask(RemotingTask task);

  void addHandler(int code, NettyRequestProcessor handler);
}
