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

package com.linkedkeeper.tcp.server;

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
