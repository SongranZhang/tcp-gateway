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
import com.linkedkeeper.tcp.connector.tcp.codec.MessageBuf;
import com.linkedkeeper.tcp.constant.Constants;
import com.linkedkeeper.tcp.message.MessageWrapper;
import com.linkedkeeper.tcp.notify.NotifyProxy;

import java.util.Map;

public class TestNotify {

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
}
