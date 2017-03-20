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

package com.linkedkeeper.tcp.notify;

import com.linkedkeeper.tcp.connector.tcp.TcpConnector;
import com.linkedkeeper.tcp.connector.tcp.codec.MessageBuf;
import com.linkedkeeper.tcp.constant.Constants;
import com.linkedkeeper.tcp.message.MessageWrapper;
import com.linkedkeeper.tcp.utils.ByteUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NotifyProxy {

    private final static Logger logger = LoggerFactory.getLogger(NotifyProxy.class);

    private TcpConnector tcpConnector;

    public NotifyProxy(TcpConnector tcpConnector) {
        this.tcpConnector = tcpConnector;
    }

    private final ConcurrentHashMap<Long, NotifyFuture> futureMap = new ConcurrentHashMap<Long, NotifyFuture>();

    public int notify(long seq, MessageWrapper wrapper, int timeout) throws Exception {
        try {
            NotifyFuture<Boolean> future = doSendAsync(seq, wrapper, timeout);
            if (future == null) {
                return Constants.NOTIFY_NO_SESSION;
            } else {
                return future.get(timeout, TimeUnit.MILLISECONDS) ? Constants.NOTIFY_SUCCESS : Constants.NOTIFY_FAILURE;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void reply(MessageBuf.JMTransfer message) throws Exception {
        try {
            long seq = message.getSeq();
            logger.info("reply seq -> " + seq + ", message -> " + ByteUtils.bytesToHexString(message.toByteArray()));
            final NotifyFuture future = this.futureMap.get(seq);
            if (future != null) {
                future.setSuccess(true);
                futureMap.remove(seq);
                logger.info("reply seq -> " + seq + " success.");
            } else {
                logger.info("reply seq -> " + seq + " expire.");
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private NotifyFuture doSendAsync(long seq, MessageWrapper wrapper, int timeout) throws Exception {
        if (wrapper == null) {
            throw new Exception("wrapper cannot be null.");
        }
        String sessionId = wrapper.getSessionId();
        if (StringUtils.isBlank(sessionId)) {
            throw new Exception("sessionId cannot be null.");
        }
        if (tcpConnector.exist(sessionId)) {
            // start.
            final NotifyFuture future = new NotifyFuture(timeout);
            this.futureMap.put(seq, future);

            logger.info("notify seq -> " + seq + ", sessionId -> " + sessionId);
            tcpConnector.send(sessionId, wrapper.getBody());

            future.setSentTime(System.currentTimeMillis()); // 置为已发送
            return future;
        } else {
            // tcpConnector not exist sessionId
            return null;
        }
    }
}
