package smartdoc.client.springmvc.handler;

import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import restdoc.client.api.model.HttpInvocation;
import restdoc.client.api.model.InvocationResult;
import restdoc.remoting.netty.NettyRequestProcessor;
import restdoc.remoting.protocol.RemotingCommand;
import restdoc.remoting.protocol.RemotingSerializable;
import restdoc.remoting.protocol.RemotingSysResponseCode;
import smartdoc.client.springmvc.SpringMVCInvokerImpl;

/** InvokerAPIHandler */
public class InvokerApiHandler implements NettyRequestProcessor {

  private final SpringMVCInvokerImpl invoker;

  @Autowired
  public InvokerApiHandler(SpringMVCInvokerImpl invoker) {
    this.invoker = invoker;
  }

  @Override
  public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)
      throws Exception {
    HttpInvocation invocation =
        RemotingSerializable.decode(request.getBody(), HttpInvocation.class);
    InvocationResult invocationResult = invoker.rpcInvoke(invocation);
    RemotingCommand response =
        RemotingCommand.createResponseCommand(RemotingSysResponseCode.SUCCESS, null);
    response.setBody(invocationResult.encode());
    return response;
  }

  @Override
  public boolean rejectRequest() {
    return false;
  }
}
