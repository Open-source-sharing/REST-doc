package smartdoc.remote;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.nio.ByteBuffer;

@ChannelHandler.Sharable
public class CommandEncoder extends MessageToByteEncoder<Command> {

  @Override
  protected void encode(ChannelHandlerContext ctx, Command cmd, ByteBuf out) throws Exception {
    try {
      ByteBuffer header = cmd.encodeHeader();
      out.writeBytes(header);
      byte[] body = cmd.getBody();
      if (body != null) {
        out.writeBytes(body);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
