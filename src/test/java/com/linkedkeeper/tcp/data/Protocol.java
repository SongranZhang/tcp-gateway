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

package com.linkedkeeper.tcp.data;

import com.linkedkeeper.tcp.connector.tcp.codec.MessageBuf;
import org.apache.commons.lang.time.DateFormatUtils;

import java.util.Date;

public class Protocol {

    public static MessageBuf.JMTransfer.Builder generateConnect() {
        MessageBuf.JMTransfer.Builder builder = MessageBuf.JMTransfer.newBuilder();
        builder.setVersion("1.0");
        builder.setDeviceId("test");
        builder.setCmd(1000);
        builder.setSeq(1234);
        builder.setFormat(1);
        builder.setFlag(1);
        builder.setPlatform("pc");
        builder.setPlatformVersion("1.0");
        builder.setToken("abc");
        builder.setAppKey("123");
        builder.setTimeStamp("123456");
        builder.setSign("123");

        Login.MessageBufPro.MessageReq.Builder logReq = Login.MessageBufPro.MessageReq.newBuilder();
        logReq.setMethod("connect");
        logReq.setToken("iosaaa");
        logReq.setParam("123");
        logReq.setSign("ios333");
        logReq.setTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        logReq.setV("1.0");
        logReq.setDevice("tcp test");
        logReq.setApp("server");
        logReq.setCmd(Login.MessageBufPro.CMD.CONNECT); // 连接

        builder.setBody(logReq.build().toByteString());

        return builder;
    }

    public static MessageBuf.JMTransfer.Builder generateHeartbeat() {
        MessageBuf.JMTransfer.Builder builder = MessageBuf.JMTransfer.newBuilder();
        builder.setVersion("1.0");
        builder.setDeviceId("test");
        builder.setCmd(1002);
        builder.setSeq(1234);
        builder.setFormat(1);
        builder.setFlag(1);
        builder.setPlatform("pc");
        builder.setPlatformVersion("1.0");
        builder.setToken("abc");
        builder.setAppKey("123");
        builder.setTimeStamp("123456");
        builder.setSign("123");

        Login.MessageBufPro.MessageReq.Builder heartbeatReq = Login.MessageBufPro.MessageReq.newBuilder();
        heartbeatReq.setMethod("123");
        heartbeatReq.setToken("iosaaa");
        heartbeatReq.setParam("123");
        heartbeatReq.setSign("ios333");
        heartbeatReq.setTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        heartbeatReq.setV("1.0");
        heartbeatReq.setDevice("tcp test");
        heartbeatReq.setApp("server");
        heartbeatReq.setCmd(Login.MessageBufPro.CMD.HEARTBEAT); // 心跳

        builder.setBody(heartbeatReq.build().toByteString());

        return builder;
    }
}
