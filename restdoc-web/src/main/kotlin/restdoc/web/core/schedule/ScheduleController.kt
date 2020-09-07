package restdoc.web.core.schedule

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import restdoc.remoting.common.RequestCode
import restdoc.remoting.common.body.GetClientApiListRequestBody
import restdoc.remoting.common.body.HttpCommunicationCapture
import restdoc.remoting.common.header.SubmitHttpTaskRequestHeader
import restdoc.remoting.data.ApiEmptyTemplate
import restdoc.remoting.exception.RemotingCommandException
import restdoc.remoting.exception.RemotingSendRequestException
import restdoc.remoting.exception.RemotingTimeoutException
import restdoc.remoting.netty.NettyRemotingServer
import restdoc.remoting.netty.NettyServerConfig
import restdoc.remoting.protocol.RemotingCommand
import restdoc.remoting.protocol.RemotingSerializable
import restdoc.remoting.protocol.RemotingSysResponseCode
import restdoc.web.core.ServiceException
import restdoc.web.core.Status

/**
 * ScheduleServer provided the tcp server dashboard
 *
 * @author ubuntu-m
 */
@Component
class ScheduleController : CommandLineRunner {

    private val log: Logger = LoggerFactory.getLogger(ScheduleController::class.java)

    private val httpTaskExecuteTimeout = (32 shl 9).toLong()

    private val thread: Thread = Thread(Runnable {
        this.remotingServer.start()
    })

    private val scheduleProperties: ScheduleProperties
    private val clientManager: ClientChannelManager
    private val reportClientInfoRequestProcessor: ReportClientInfoRequestProcessor

    private val remotingServer: NettyRemotingServer;

    @Autowired
    constructor(scheduleProperties: ScheduleProperties,
                clientManager: ClientChannelManager,
                reportClientInfoRequestProcessor: ReportClientInfoRequestProcessor) {

        this.scheduleProperties = scheduleProperties
        this.clientManager = clientManager
        this.reportClientInfoRequestProcessor = reportClientInfoRequestProcessor

        val config = NettyServerConfig()
        config.listenPort = scheduleProperties.port
        remotingServer = NettyRemotingServer(config)
    }


    fun initialize() {
        this.remotingServer.registerProcessor(RequestCode.REPORT_CLIENT_INFO, reportClientInfoRequestProcessor, null);
    }

    override fun run(vararg args: String?) {
        this.initialize()

        this.thread.start()

        log.info("ScheduleController started")
    }

    @Throws(InterruptedException::class,
            RemotingTimeoutException::class,
            RemotingSendRequestException::class,
            RemotingCommandException::class)
    fun syncSubmitRemoteHttpTask(clientId: String?,
                                 taskId: String?,
                                 capture: HttpCommunicationCapture): HttpCommunicationCapture {

        val header = SubmitHttpTaskRequestHeader()
        header.taskId = taskId
        val request = RemotingCommand.createRequestCommand(RequestCode.SUBMIT_HTTP_PROCESS, header)
        request.body = capture.encode()
        val clientChannelInfo = clientManager.findClient(clientId)

        val response = remotingServer.invokeSync(clientChannelInfo!!.channel, request,
                this.httpTaskExecuteTimeout)

        return if (response.code == RemotingSysResponseCode.SUCCESS) {
            val responseBody = RemotingSerializable.decode(response.body,
                    HttpCommunicationCapture::class.java)

            responseBody
        } else {
            throw ServiceException(response.remark, Status.INTERNAL_SERVER_ERROR)
        }
    }

    fun syncGetEmptyApiTemplates(clientId: String?): List<ApiEmptyTemplate> {

        val request = RemotingCommand.createRequestCommand(RequestCode.GET_EMPTY_API_TEMPLATES, null)

        val clientChannelInfo = clientManager.findClient(clientId)

        val response = remotingServer.invokeSync(clientChannelInfo!!.channel, request,
                this.httpTaskExecuteTimeout)

        return if (response.code == RemotingSysResponseCode.SUCCESS) {
            (RemotingSerializable.decode(response.body,
                    GetClientApiListRequestBody::class.java) as GetClientApiListRequestBody).apiEmptyTemplates
        } else {
            throw ServiceException(response.remark, Status.INTERNAL_SERVER_ERROR)
        }
    }
}