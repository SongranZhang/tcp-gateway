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

package com.linkedkeeper.tcp.connector.tcp;

import com.linkedkeeper.tcp.connector.api.ExchangeConnection;
import com.linkedkeeper.tcp.exception.LostConnectException;
import com.linkedkeeper.tcp.exception.PushException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpConnection<T> extends ExchangeConnection<T> {

    private final static Logger logger = LoggerFactory.getLogger(TcpConnection.class);

    private ChannelHandlerContext cxt;

    public TcpConnection(ChannelHandlerContext cxt) {
        this.cxt = cxt;
    }

    public void connect() {
    }

    public void close() {
        this.close = true;

        cxt.close();
        logger.debug("the connection have been destroyed! ctx -> " + cxt.toString());
    }

    public void send(T message) {
        if (message == null)
            return;

        sendMessage(message);
    }

    private void sendMessage(T message) {
        if (isClosed()) {
            PushException e = new PushException("Use a closed pushSocked!");
            this.fireError(e);
            return;
        }
        pushMessage0(message);
    }

    private void pushMessage0(T message) {
        try {
            ChannelFuture cf = cxt.writeAndFlush(message);
            cf.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws PushException {
                    if (future.isSuccess()) {
                        logger.debug("send success.");
                    } else {
                        throw new PushException("Failed to send message.");
                    }
                    Throwable cause = future.cause();
                    if (cause != null) {
                        throw new PushException(cause);
                    }
                }
            });
        } catch (LostConnectException e) {
            logger.error("TcpConnection pushMessage occur LostConnectException.", e);
            this.fireError(new PushException(e));
        } catch (Exception e) {
            logger.error("TcpConnection pushMessage occur Exception.", e);
            this.fireError(new PushException("ChannelFuture " + connectionId + " ", e));
        } catch (Throwable e) {
            logger.error("TcpConnection pushMessage occur Throwable.", e);
            this.fireError(new PushException("Failed to send message, cause: " + e.getMessage(), e));
        }
    }

    private void pushMessage(T message) {
        boolean success = true;
        boolean sent = true;
        int timeout = 60;
        try {
            ChannelFuture cf = cxt.write(message);
            cxt.flush();
            if (sent) {
                success = cf.await(timeout);
            }
            if (cf.isSuccess()) {
                logger.debug("send success.");
            }
            Throwable cause = cf.cause();
            if (cause != null) {
                this.fireError(new PushException(cause));
            }
        } catch (LostConnectException e) {
            logger.error("TcpConnection pushMessage occur LostConnectException.", e);
            this.fireError(new PushException(e));
        } catch (Exception e) {
            logger.error("TcpConnection pushMessage occur Exception.", e);
            this.fireError(new PushException("ChannelFuture " + connectionId + " ", e));
        } catch (Throwable e) {
            logger.error("TcpConnection pushMessage occur Throwable.", e);
            this.fireError(new PushException("Failed to send message, cause: " + e.getMessage(), e));
        }
        if (!success) {
            this.fireError(new PushException("Failed to send message, in timeout(" + timeout + "ms) limit"));
        }
    }
}
