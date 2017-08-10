# tcp-gateway
High performance TCP Gateway base on Netty 4 ,for request data or push message.

# Installation
Clone this repository, and add it as a dependent maven project.

# Usage
## Create a Tcp Server
### Config spring-tcp-server.xml to start server
```xml 
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
	               http://www.springframework.org/schema/beans/spring-beans.xsd"
       default-autowire="byName">

    <!-- tcp server config start. -->
    <bean id="tcpServer" class="com.linkedkeeper.tcp.connector.tcp.server.TcpServer" init-method="init"
          destroy-method="shutdown">
        <!-- port is tcp server port -->
        <property name="port" value="2000"/>
    </bean>
    <bean id="tcpSessionManager" class="com.linkedkeeper.tcp.connector.tcp.TcpSessionManager">
        <property name="maxInactiveInterval" value="500"/>
        <!-- you can add listener to listen session event, include session create, destroy and so on. -->
        <property name="sessionListeners">
            <list>
                <ref bean="logSessionListener"/>
            </list>
        </property>
    </bean>
    <!-- logSessionListener is related tcpSessionManager, those listener should implements SessionListener -->
    <bean id="logSessionListener" class="com.linkedkeeper.tcp.connector.api.listener.LogSessionListener"/>
    <!-- tcp sender is a container that can send message to client from server -->
    <bean id="tcpSender" class="com.linkedkeeper.tcp.remoting.TcpSender">
        <constructor-arg ref="tcpConnector"/>
    </bean>
    <!-- server config is combine the config, don't modify -->
    <bean id="serverConfig" class="com.linkedkeeper.tcp.connector.tcp.config.ServerTransportConfig">
        <constructor-arg ref="tcpConnector"/>
        <constructor-arg ref="proxy"/>
        <constructor-arg ref="notify"/>
    </bean>
    <!-- tcp connector is container that manage the connection between server and client -->
    <bean id="tcpConnector" class="com.linkedkeeper.tcp.connector.tcp.TcpConnector" init-method="init"
          destroy-method="destroy"/>
    <!-- notify proxy is proxy that implement send notify to client -->
    <bean id="notify" class="com.linkedkeeper.tcp.notify.NotifyProxy">
        <constructor-arg ref="tcpConnector"/>
    </bean>
    <!-- default tcp server config end. -->
    
    <!-- this proxy is your proxy that can receive message from client -->
    <bean id="proxy" class="com.linkedkeeper.tcp.server.TestSimpleProxy"/>
</beans>
```
* tcpServer: provide tcp connection service.
* tcpSessionManager: you can add listener to listen session event, include session create, destroy and so on.
* logSessionListener: it is related tcpSessionManager, those listener should implement SessionListener.
* tcpSender: it is a container that can send message to client from server.
* serverConfig: it is combine the config.
* tcpConnector: it is container that manage the connection between server and client.
* notifyProxy: it is proxy that implement send notify to client.

Above config is default, you don't have to change it. But you can change port.
### Create Test Proxy to receive message from client
```java 
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.linkedkeeper.tcp.connector.tcp.codec.MessageBuf;
import com.linkedkeeper.tcp.data.Login;
import com.linkedkeeper.tcp.data.Protocol;
import com.linkedkeeper.tcp.invoke.ApiProxy;
import com.linkedkeeper.tcp.message.MessageWrapper;
import com.linkedkeeper.tcp.message.SystemMessage;

public class TestSimpleProxy implements ApiProxy {

    public MessageWrapper invoke(SystemMessage sMsg, MessageBuf.JMTransfer message) {
        ByteString body = message.getBody();

        if (message.getCmd() == 1000) {
            try {
                Login.MessageBufPro.MessageReq messageReq = Login.MessageBufPro.MessageReq.parseFrom(body);
                if (messageReq.getCmd().equals(Login.MessageBufPro.CMD.CONNECT)) {
                    return new MessageWrapper(MessageWrapper.MessageProtocol.CONNECT, message.getToken(), null);
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        } else if (message.getCmd() == 1002) {
            try {
                Login.MessageBufPro.MessageReq messageReq = Login.MessageBufPro.MessageReq.parseFrom(body);
                if (messageReq.getCmd().equals(Login.MessageBufPro.CMD.HEARTBEAT)) {
                    MessageBuf.JMTransfer.Builder resp = Protocol.generateHeartbeat();
                    return new MessageWrapper(MessageWrapper.MessageProtocol.HEART_BEAT, message.getToken(), resp);
                }
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
```
#### Input Parameters:
* SystemMessage: the message get from tcp server, include remoteAddress, localAddress.
* MessageBuf.JMTransfer: this is important, this class is created by protobuf, it include header and body, header is app information, you can get the detail from this project.
#### Output Parameter:
* MessageWrapper: it is message response wrapper. It include protocol, sessionId and body. Body is response data.
body is byte type, and it also a protobuf bytes.
### Send Notify to Client
```java 
private NotifyProxy notify;

final int timeout = 10 * 1000;
final int NOTIFY = 3;

public boolean send(long seq, String sessionId, int cmd, ByteString body) throws Exception {
    boolean success = false;
    MessageBuf.JMTransfer.Builder builder = generateNotify(sessionId, seq, cmd, body);
    if (builder != null) {
        MessageWrapper wrapper = new MessageWrapper(MessageWrapper.MessageProtocol.NOTIFY, sessionId, builder);
        int ret = notify.notify(seq, wrapper, timeout);
        if (ret == Constants.NOTIFY_SUCCESS) {
            success = true;
        } else if (ret == Constants.NOTIFY_NO_SESSION) {
            /** no session on this machine **/
            success = true;
        }
    } else {
        /** no session in the cache **/
        success = true;
    }
    return success;
}

/**
 * session
 */
final String VERSION = "version";
final String DEVICE_ID = "deviceId";
final String PLATFORM = "platform";
final String PLATFORM_VERSION = "platformVersion";
final String TOKEN = "token";
final String APP_KEY = "appKey";
final String TIMESTAMP = "timestamp";
final String SIGN = "sign";

/**
 * need session into redis, then when you notify you can get info from redis by session
 */
final Map<String, Map<String, Object>> testSessionMap = null;

protected MessageBuf.JMTransfer.Builder generateNotify(String sessionId, long seq, int cmd, ByteString body) throws Exception {
    Map<String, Object> map = testSessionMap.get(sessionId);

    MessageBuf.JMTransfer.Builder builder = MessageBuf.JMTransfer.newBuilder();
    builder.setVersion(String.valueOf(map.get(VERSION)));
    builder.setDeviceId(String.valueOf(map.get(DEVICE_ID)));
    builder.setCmd(cmd);
    builder.setSeq(seq);
    builder.setFormat(NOTIFY);
    builder.setFlag(0);
    builder.setPlatform(String.valueOf(map.get(PLATFORM)));
    builder.setPlatformVersion(String.valueOf(map.get(PLATFORM_VERSION)));
    builder.setToken(String.valueOf(map.get(TOKEN)));
    builder.setAppKey(String.valueOf(map.get(APP_KEY)));
    builder.setTimeStamp(String.valueOf(map.get(TIMESTAMP)));
    builder.setSign(String.valueOf(map.get(SIGN)));
    builder.setBody(body);

    return builder;
}
```
## Tcp Client 
support iOS, android, C++ languages 
### Serialize protobuf
#### java
```bat 
/protobuf/protoc --proto_path=/protobuf/ --java_out=/protobuf/MessageBuf.proto
```
#### object-c
```bat 
protoc --plugin=/protobuf/protoc-gen-objc MessageBuf.proto --object_out="/protobuf/"
```

Notes. You can add QQ group to download protobuf compiler