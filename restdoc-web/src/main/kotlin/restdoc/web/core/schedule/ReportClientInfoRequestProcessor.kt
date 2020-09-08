package restdoc.web.core.schedule

import io.netty.channel.ChannelHandlerContext
import org.springframework.stereotype.Component
import restdoc.remoting.ClientChannelInfo
import restdoc.remoting.common.body.ClientInfoBody
import restdoc.remoting.netty.NettyRequestProcessor
import restdoc.remoting.protocol.LanguageCode
import restdoc.remoting.protocol.RemotingCommand
import restdoc.remoting.protocol.RemotingSerializable
import restdoc.remoting.protocol.RemotingSysResponseCode
import java.net.InetSocketAddress

@Component
class ReportClientInfoRequestProcessor(private val clientManager: ClientChannelManager) : NettyRequestProcessor {

    override fun rejectRequest(): Boolean {
        return false
    }

    override fun processRequest(ctx: ChannelHandlerContext, request: RemotingCommand): RemotingCommand {
        val body = RemotingSerializable.decode(request.body, ClientInfoBody::class.java)

        val address = ctx.channel().remoteAddress() as InetSocketAddress

        val clientChannelInfo = ClientChannelInfo(
                ctx.channel(),
                address.address.hostAddress,
                LanguageCode.JAVA,
                1)

        clientChannelInfo.hostname = body.hostname
        clientChannelInfo.osname = body.osname
        clientChannelInfo.service = body.service

        clientManager.registerClient(clientChannelInfo.clientId, clientChannelInfo)

        return RemotingCommand.createResponseCommand(RemotingSysResponseCode.SUCCESS, null)
    }
}