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
