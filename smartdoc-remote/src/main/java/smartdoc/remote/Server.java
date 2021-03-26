package smartdoc.remote;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

public abstract class Server extends AbstractService {

  private final ServerConfig cfg;
  private ServerBootstrap bootstrap;
  private EventLoopGroup eventLoopGroupSelector;
  private EventLoopGroup eventLoopGroupBoss;
  private DefaultEventExecutorGroup defaultEventExecutorGroup;

  private List<Callable<Boolean>> initedCallbacks = new ArrayList<>();
  private List<Callable<Boolean>> startedCallbacks = new ArrayList<>();
  private List<Callable<Boolean>> stoppedCallbacks = new ArrayList<>();

  public abstract int port();

  Server(ServerConfig cfg) throws Exception {
    this.cfg = cfg;
    this.status = Status.Init;
    setupConfig();
    runCallbacks(initedCallbacks);
  }

  @Override
  public synchronized void start() throws ServiceException {

    ServerBootstrap childHandler =
        this.bootstrap
            .group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
            .channel(cfg.useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, 1024)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_SNDBUF, cfg.getServerSocketSndBufSize())
            .childOption(ChannelOption.SO_RCVBUF, cfg.getServerSocketRcvBufSize())
            .localAddress(new InetSocketAddress(this.port()))
            .childHandler(
                new ChannelInitializer<SocketChannel>() {
                  @Override
                  public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast(defaultEventExecutorGroup, HANDSHAKE_HANDLER_NAME, handshakeHandler)
                        .addLast(
                            defaultEventExecutorGroup,
                            encoder,
                            new RemotingCommandDecoder(),
                            new IdleStateHandler(
                                0, 0, nettyServerConfig.getServerChannelMaxIdleTimeSeconds()),
                            connectionManageHandler,
                            serverHandler);
                  }
                });

    this.status = Status.Started;
    try {
      runCallbacks(startedCallbacks);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServiceException();
    }
  }

  @Override
  public synchronized void stop() throws ServiceException {
    this.status = Status.Stopped;
    try {
      runCallbacks(stoppedCallbacks);
    } catch (Exception e) {
      e.printStackTrace();
      throw new ServiceException();
    }
  }

  public void addInitCallback(Callable<Boolean> callback) {
    this.initedCallbacks.add(callback);
  }

  public void addStartedCallback(Callable<Boolean> callback) {
    this.startedCallbacks.add(callback);
  }

  private void runCallbacks(List<Callable<Boolean>> callbacks) throws Exception {
    for (Callable<Boolean> callback : callbacks) {
      callback.call();
    }
  }

  private void setupConfig() {
    bootstrap = new ServerBootstrap();
    defaultEventExecutorGroup = new DefaultEventExecutorGroup(cfg.getServerWorkerThreads(),
        new DefaultThreadFactory("NettyServerCodecThread_%d"));

    int selectorThreadNumber = cfg.getServerSelectorThreads();

    if (cfg.useEpoll()) {
      eventLoopGroupBoss = new EpollEventLoopGroup(1, new DefaultThreadFactory("EPOLLBoss_%d"));
      eventLoopGroupSelector = new EpollEventLoopGroup(selectorThreadNumber,
          new DefaultThreadFactory("EPOLLBoss_" + selectorThreadNumber));
    } else {
      eventLoopGroupBoss = new NioEventLoopGroup(1, new DefaultThreadFactory("NettyNIOBoss_%d"));
      eventLoopGroupSelector = new NioEventLoopGroup(selectorThreadNumber, new DefaultThreadFactory("NettyServerNIOSelector_%d"));
    }
  }

  @ChannelHandler.Sharable
  class HandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final TlsMode tlsMode;

    private static final byte HANDSHAKE_MAGIC_CODE = 0x16;

    HandshakeHandler(TlsMode tlsMode) {
      this.tlsMode = tlsMode;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

      // mark the current position so that we can peek the first byte to determine if the content is
      // starting with
      // TLS handshake
      msg.markReaderIndex();

      byte b = msg.getByte(0);

      if (b == HANDSHAKE_MAGIC_CODE) {
        switch (tlsMode) {
          case DISABLED:
            ctx.close();
            log.warn(
                "Clients intend to establish an SSL connection while this server is running in SSL"
                    + " disabled mode");
            break;
          case PERMISSIVE:
          case ENFORCING:
            if (null != sslContext) {
              ctx.pipeline()
                  .addAfter(
                      defaultEventExecutorGroup,
                      HANDSHAKE_HANDLER_NAME,
                      TLS_HANDLER_NAME,
                      sslContext.newHandler(ctx.channel().alloc()))
                  .addAfter(
                      defaultEventExecutorGroup,
                      TLS_HANDLER_NAME,
                      FILE_REGION_ENCODER_NAME,
                      new FileRegionEncoder());
              log.info("Handlers prepended to channel pipeline to establish SSL connection");
            } else {
              ctx.close();
              log.error("Trying to establish an SSL connection but sslContext is null");
            }
            break;

          default:
            log.warn("Unknown TLS mode");
            break;
        }
      } else if (tlsMode == TlsMode.ENFORCING) {
        ctx.close();
        log.warn(
            "Clients intend to establish an insecure connection while this server is running in"
                + " SSL enforcing mode");
      }

      // reset the reader index so that handshake negotiation may proceed as normal.
      msg.resetReaderIndex();

      try {
        // Remove this handler
        ctx.pipeline().remove(this);
      } catch (NoSuchElementException e) {
        log.error("Error while removing HandshakeHandler", e);
      }

      // Hand over this message to the next .
      ctx.fireChannelRead(msg.retain());
    }
  }
}