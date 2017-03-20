/*
 * Copyright (c) 2016, LinkedKeeper
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of LinkedKeeper nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.linkedkeeper.tcp.connector.tcp.server;

import com.linkedkeeper.tcp.connector.tcp.TcpConnector;
import com.linkedkeeper.tcp.connector.tcp.codec.MessageBuf;
import com.linkedkeeper.tcp.connector.tcp.config.ServerTransportConfig;
import com.linkedkeeper.tcp.constant.Constants;
import com.linkedkeeper.tcp.invoke.ApiProxy;
import com.linkedkeeper.tcp.message.MessageWrapper;
import com.linkedkeeper.tcp.message.SystemMessage;
import com.linkedkeeper.tcp.notify.NotifyProxy;
import com.linkedkeeper.tcp.utils.NetUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

//public class TcpServerHandler extends ChannelHandlerAdapter {
@ChannelHandler.Sharable
public class TcpServerHandler extends ChannelInboundHandlerAdapter {

    private final static Logger logger = LoggerFactory.getLogger(TcpServerHandler.class);

    private TcpConnector tcpConnector = null;
    private ApiProxy proxy = null;
    private NotifyProxy notify = null;

    public TcpServerHandler(ServerTransportConfig config) {
        this.tcpConnector = config.getTcpConnector();
        this.proxy = config.getProxy();
        this.notify = config.getNotify();
    }

    public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
        try {
            if (o instanceof MessageBuf.JMTransfer) {
                SystemMessage sMsg = generateSystemMessage(ctx);
                MessageBuf.JMTransfer message = (MessageBuf.JMTransfer) o;
                // inbound
                if (message.getFormat() == SEND) {
                    MessageWrapper wrapper = proxy.invoke(sMsg, message);
                    if (wrapper != null)
                        this.receive(ctx, wrapper);
                }
                // outbound
                if (message.getFormat() == REPLY) {
                    notify.reply(message);
                }
            } else {
                logger.warn("TcpServerHandler channelRead message is not proto.");
            }
        } catch (Exception e) {
            logger.error("TcpServerHandler TcpServerHandler handler error.", e);
            throw e;
        }
    }

    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.debug("TcpServerHandler Connected from {" +
                NetUtils.channelToString(ctx.channel().remoteAddress(), ctx.channel().localAddress()) + "}");
    }

    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.debug("TcpServerHandler Disconnected from {" +
                NetUtils.channelToString(ctx.channel().remoteAddress(), ctx.channel().localAddress()) + "}");
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        logger.debug("TcpServerHandler channelActive from (" + getRemoteAddress(ctx) + ")");
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        logger.debug("TcpServerHandler channelInactive from (" + getRemoteAddress(ctx) + ")");
        String sessionId0 = getChannelSessionHook(ctx);
        if (StringUtils.isNotBlank(sessionId0)) {
            tcpConnector.close(new MessageWrapper(MessageWrapper.MessageProtocol.CLOSE, sessionId0, null));
            logger.warn("TcpServerHandler channelInactive, close channel sessionId0 -> " + sessionId0 + ", ctx -> " + ctx.toString());
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("TcpServerHandler (" + getRemoteAddress(ctx) + ") -> Unexpected exception from downstream." + cause);
        String sessionId0 = getChannelSessionHook(ctx);
        if (StringUtils.isNotBlank(sessionId0)) {
            logger.error("TcpServerHandler exceptionCaught (sessionId0 -> " + sessionId0 + ", ctx -> " + ctx.toString() + ") -> Unexpected exception from downstream." + cause);
        }
    }

    private String getChannelSessionHook(ChannelHandlerContext ctx) {
        return ctx.channel().attr(Constants.SERVER_SESSION_HOOK).get();
    }

    private void setChannelSessionHook(ChannelHandlerContext ctx, String sessionId) {
        ctx.channel().attr(Constants.SERVER_SESSION_HOOK).set(sessionId);
    }

    final int SEND = 1;
    final int RECEIVE = 2;
    final int NOTIFY = 3;
    final int REPLY = 4;

    /**
     * to send client and receive the message
     *
     * @param ctx
     * @param wrapper
     */
    private void receive(ChannelHandlerContext ctx, MessageWrapper wrapper) {
        if (wrapper.isConnect()) {
            isConnect0(ctx, wrapper);
        } else if (wrapper.isClose()) {
            tcpConnector.close(wrapper);
        } else if (wrapper.isHeartbeat()) {
            tcpConnector.heartbeatClient(wrapper);
        } else if (wrapper.isSend()) {
            tcpConnector.responseSendMessage(wrapper);
        } else if (wrapper.isNoKeepAliveMessage()) {
            tcpConnector.responseNoKeepAliveMessage(ctx, wrapper);
        }
    }

    private void isConnect0(ChannelHandlerContext ctx, MessageWrapper wrapper) {
        String sessionId = wrapper.getSessionId();
        String sessionId0 = getChannelSessionHook(ctx);
        if (sessionId.equals(sessionId0)) {
            logger.info("tcpConnector reconnect sessionId -> " + sessionId + ", ctx -> " + ctx.toString());
            tcpConnector.responseSendMessage(wrapper);
        } else {
            logger.info("tcpConnector connect sessionId -> " + sessionId + ", sessionId0 -> " + sessionId0 + ", ctx -> " + ctx.toString());
            tcpConnector.connect(ctx, wrapper);
            setChannelSessionHook(ctx, sessionId);
            logger.info("create channel attr sessionId " + sessionId + " successful, ctx -> " + ctx.toString());
        }
    }

    private SystemMessage generateSystemMessage(ChannelHandlerContext ctx) {
        SystemMessage systemMessage = new SystemMessage();
        systemMessage.setRemoteAddress(getRemoteAddress(ctx));
        systemMessage.setLocalAddress(getLocalAddress(ctx));

        return systemMessage;
    }

    private String getRemoteAddress(ChannelHandlerContext ctx) {
        SocketAddress remote1 = ctx.channel().remoteAddress();
        InetSocketAddress remote = (InetSocketAddress) remote1;
        return NetUtils.toAddressString(remote);
    }

    private String getLocalAddress(ChannelHandlerContext ctx) {
        SocketAddress local1 = ctx.channel().localAddress();
        InetSocketAddress local = (InetSocketAddress) local1;
        return NetUtils.toAddressString(local);
    }
}
