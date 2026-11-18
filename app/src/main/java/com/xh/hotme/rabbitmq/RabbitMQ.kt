package com.xh.hotme.rabbitmq

import android.text.TextUtils
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONException
import com.alibaba.fastjson.JSONObject
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.xh.hotme.HotmeApplication
import com.xh.hotme.account.LoginManager
import com.xh.hotme.bluetooth.BluetoothHandle
import com.xh.hotme.bluetooth.bean.BaseBluetoothBean
import com.xh.hotme.utils.AppTrace
import com.xh.hotme.utils.Constants
import com.xh.hotme.utils.GsonUtils
import okio.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RabbitMQ private constructor() : Runnable {
    companion object {
        private val TAG = RabbitMQ::class.java.simpleName
        private val HOST_DEV = "mq.test.rewoo360.com"
        private val HOST_PROD = "mq.rewoo360.com"
        private val PORT_DEV = 5672
        private val PORT_PROD = 5672
        private val HOST_GZT = "223.109.239.30"
        private val PORT_GZT = 61003
        private val VHOST = "rewoo"
        private val EXCHANGE = "headers-exchange"
        private val USER = "rewoo"
        private val PWD = "mq@rewoo360.com"

        private val executor = Executors.newSingleThreadScheduledExecutor()

        @Volatile
        private var inst: RabbitMQ? = null

        fun getImpl() = inst ?: synchronized(this) {
            inst ?: RabbitMQ().also { inst = it }
        }

        fun launch() {
            executor.schedule(getImpl(), 1, TimeUnit.SECONDS)
        }
    }

    private val _factory: ConnectionFactory = ConnectionFactory()
    private var _connection: Connection? = null
    private var _channel: Channel? = null
    private var _queueName: String? = null
    private var _errMsg: String? = null

    private enum class MQState {
        INIT,
        CREATING_CONNECTION,
        CONNECTION_READY,
        CREATING_CHANNEL,
        CHANNEL_READY,
        CREATING_QUEUE,
        QUEUE_READY,
        CREATING_EXCHANGE,
        EXCHANGE_READY,
        CREATING_BINDING,
        BINDING_READY
    }

    private var _state: MQState = MQState.INIT

    // 发送队列, key是device id, value是json字符串list
    private val _sendQueue = mutableMapOf<String, MutableList<String>>()

    @Volatile private var reloadRequested = false
    @Volatile private var environmentGeneration = 0
    private var configuredGeneration = 0

    fun reloadEnvironment() {
        environmentGeneration++
        synchronized(this) { _sendQueue.clear() }
        reloadRequested = true
    }

    private fun configureEnvironment() {
        val uri = java.net.URI(com.xh.hotme.environment.RuntimeEnvironment.get().rabbitmq)
        _factory.host = uri.host.removePrefix("[").removeSuffix("]")
        _factory.port = if (uri.port < 0) 5672 else uri.port
        _factory.virtualHost = uri.path.removePrefix("/")
        configuredGeneration = environmentGeneration
        _factory.username = USER
        _factory.password = PWD
    }

    init {
        _factory.host = if (Constants.isGztCrew()) HOST_GZT else if (Constants.TEST_SERVER) HOST_DEV else HOST_PROD
        _factory.port = if (Constants.isGztCrew()) PORT_GZT else if (Constants.TEST_SERVER) PORT_DEV else PORT_PROD
        _factory.virtualHost = VHOST
        _factory.username = USER
        _factory.password = PWD
        configureEnvironment()
    }

    override fun run() {
        try {
            while (!Thread.currentThread().isInterrupted) {
                Thread.sleep(1000)

                if (reloadRequested) {
                    reloadRequested = false
                    closeConnection()
                    configureEnvironment()
                }

                // handle by state
                when (_state) {
                    MQState.INIT -> createConnection()
                    MQState.CONNECTION_READY -> createChannel()
                    MQState.CHANNEL_READY -> createQueue()
                    MQState.QUEUE_READY -> createExchange()
                    MQState.EXCHANGE_READY -> createBinding()
                    MQState.BINDING_READY -> {
                        // 发消息
                        synchronized(this) {
                            _sendQueue.forEach { deviceId, v ->
                                v.forEach {
                                    publishMessage(deviceId, it)
                                }
                                v.clear()
                            }
                        }
                    }
                    else -> {
                        // nothing to do
                    }
                }
            }
        } catch (e: Exception) {
            AppTrace.d(TAG, "thread interrupted: ${e.localizedMessage}, will re-launch")
            closeConnection()
        }
    }

    private fun isJSONValid(str: String): Boolean {
        try {
            JSONObject.parseObject(str)
        } catch (ex: JSONException) {
            try {
                JSONObject.parseArray(str)
            } catch (ex1: JSONException) {
                return false
            }
        }
        return true
    }

    fun sendCmdSimpleObj(deviceId: String, bean: BaseBluetoothBean) {
        val jsonStr = GsonUtils.GsonString(bean)
        sendMessage(deviceId, jsonStr)
    }

    fun sendMessage(deviceId: String, msg: String) {
        synchronized(this) {
            var queue = _sendQueue.get(deviceId)
            if (queue == null) {
                queue = mutableListOf()
                _sendQueue.put(deviceId, queue)
            }
            queue.add(msg)
        }
    }

    private fun publishMessage(deviceId: String, msg: String) {
        _channel?.let {
            try {
                val props = AMQP.BasicProperties.Builder()
                    .headers(mapOf(
                        "messageReceiver" to "camera-$deviceId",
                        "messageSender" to _queueName,
                        "messageReplyTo" to _queueName
                    ))
                    .expiration("30000")
                    .deliveryMode(2) // persistent
                    .build()
                it.basicPublish(EXCHANGE, "", props, msg.toByteArray(StandardCharsets.UTF_8))
                AppTrace.d(TAG, "send to device($deviceId) ok: $msg")
            } catch (e: Exception) {
                AppTrace.d(
                    TAG,
                    "failed to send msg to device($deviceId), msg: $msg, error: ${e.localizedMessage}"
                )

                // 发送消息失败太多次, 进行重连
                closeConnection()
            }
        }
    }

    private fun closeConnection() {
        _connection?.let {
            _channel?.let {
                try {
                    it.close()
                } catch (e: Exception) {
                    AppTrace.d(TAG, "failed to close channel: ${e.localizedMessage}")
                }
            }
            try {
                it.close()
            } catch (e: Exception) {
                AppTrace.d(TAG, "failed to close connection: ${e.localizedMessage}")
            }
        }
        _channel = null
        _connection = null
        _state = MQState.INIT
        AppTrace.d(TAG, "rabbitmq state back to init, will re-create connection")
    }

    private fun createBinding() {
        _channel?.let {
            _state = MQState.CREATING_BINDING
            try {
                it.queueBind(_queueName, EXCHANGE, "", mapOf("messageReceiver" to _queueName))
                _state = MQState.BINDING_READY
                AppTrace.d(
                    TAG,
                    "queue $_queueName is bound to $EXCHANGE successfully, ready to send/recv messages!"
                )

                // 收消息
                _channel?.let {
                    try {
                        val generation = configuredGeneration
                        it.basicConsume(
                            _queueName,
                            true,
                            { consumerTag, delivery ->
                                val message = String(delivery.body, StandardCharsets.UTF_8)
                                if (generation == environmentGeneration && !reloadRequested && isJSONValid(message)) {
                                    AppTrace.d(TAG, "recv message: $message")
                                    val jsonRoot = JSON.parse(message) as JSONObject
                                    BluetoothHandle.handle(jsonRoot, null)
                                } else {
                                    AppTrace.d(TAG, "recv invalid message: $message")
                                }
                            },
                            { consumerTag ->
                                // nothing to do
                            }
                        )
                    } catch (e: Exception) {
                        AppTrace.d(TAG, "failed to consume: ${e.localizedMessage}, will re-init")
                        closeConnection()
                    }
                }
            } catch (e: Exception) {
                AppTrace.d(TAG, "failed to create binding: ${e.localizedMessage}, will re-init")
                closeConnection()
            }
        }
    }

    private fun createExchange() {
        _channel?.let {
            _state = MQState.CREATING_EXCHANGE
            try {
                it.exchangeDeclare(EXCHANGE, BuiltinExchangeType.HEADERS, true)
                _state = MQState.EXCHANGE_READY
                AppTrace.d(TAG, "exchange $EXCHANGE is created successfully")
            } catch (e: Exception) {
                AppTrace.d(TAG, "failed to create exchange: ${e.localizedMessage}, will re-init")
                closeConnection()
            }
        }
    }

    private fun createQueue() {
        _channel?.let {
            // user must be logged in
            val userInfo = LoginManager.loginUserInfo
            if (userInfo == null) {
                _errMsg = "user is not logged in, can not create queue"
                AppTrace.d(TAG, _errMsg)
                return
            }

            // build queue name and declare it
            _state = MQState.CREATING_QUEUE
            val oaid = HotmeApplication.mOAID
            if (!TextUtils.isEmpty(oaid)) {
                _queueName = "app-${userInfo.phone}-${oaid}"
            } else {
                _queueName = "app-${userInfo.phone}"
            }
            try {
                val ok = it.queueDeclare(_queueName, true, false, false, emptyMap())
                if (ok.queue == _queueName) {
                    _state = MQState.QUEUE_READY
                    AppTrace.d(TAG, "queue $_queueName is created successfully")
                } else {
                    _state = MQState.CHANNEL_READY
                    _errMsg = "failed to create queue, returned queue name is not same as requested"
                    AppTrace.d(TAG, _errMsg)
                }
            } catch (e: Exception) {
                AppTrace.d(TAG, "failed to create queue: ${e.localizedMessage}, will re-init")
                closeConnection()
            }
        }
    }

    private fun createChannel() {
        _connection?.let {
            _state = MQState.CREATING_CHANNEL
            try {
                _channel = it.createChannel()
                if (_channel != null) {
                    _state = MQState.CHANNEL_READY
                    AppTrace.d(TAG, "channel is created successfully")
                } else {
                    _state = MQState.CONNECTION_READY
                    _errMsg = "failed to create channel, unknown reason"
                    AppTrace.d(TAG, _errMsg)
                }
            } catch (e: Exception) {
                AppTrace.d(TAG, "failed to create channel: ${e.localizedMessage}, will re-init")
                closeConnection()
            }
        }
    }

    private fun createConnection() {
        _state = MQState.CREATING_CONNECTION
        try {
            _connection = _factory.newConnection()
            _state = MQState.CONNECTION_READY
            AppTrace.d(TAG, "rabbitmq connection to ${_factory.host}:${_factory.port} is successful")
        } catch (e: Exception) {
            AppTrace.d(TAG, "failed to create connection: ${e.localizedMessage}, will re-init")
            closeConnection()
        }
    }
}
