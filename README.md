# tcp-gateway
High performance TCP Gateway base on Netty 4 ,for request data or push message.

# Installation
Clone this repository, and add it as a dependent maven project.

# Usage
## Create a Tcp Server
### Config spring-tcp-server.xml
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
tcpServer: provide tcp connection service.
tcpSessionManager: you can add listener to listen session event, include session create, destroy and so on.
logSessionListener: it is related tcpSessionManager, those listener should implements SessionListener.
tcpSender: it is a container that can send message to client from server.
serverConfig: it is combine the config.
tcpConnector: it is container that manage the connection between server and client.
notifyProxy: it is proxy that implement send notify to client.

testSimpleProxy: this proxy is your proxy that can receive message from client.