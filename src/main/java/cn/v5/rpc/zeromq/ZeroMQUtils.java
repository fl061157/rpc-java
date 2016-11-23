package cn.v5.rpc.zeromq;

import java.io.UnsupportedEncodingException;

public class ZeroMQUtils {

    public static byte[] getRequestData(String topic, byte[] data) {
        try {
            byte[] topicBytes = topic.getBytes("UTF-8");
            int topicBytesLen = topicBytes.length;
            if (topicBytesLen > 1024){
                topicBytesLen = 1024;
            }

            byte[] ret = new byte[2 + topicBytesLen + data.length];
            ret[0] = (byte)(topicBytesLen >>> 8 & 0xFF);
            ret[1] = (byte)(topicBytesLen & 0xFF);
            System.arraycopy(topicBytes, 0, ret, 2, topicBytesLen);
            System.arraycopy(data, 0, ret, 2+topicBytesLen, data.length);
            return ret;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ZeroMQRequestData getZeroMQRequestData(byte[] data) {
        int topicBytesLen = data[0] * 256 + data[1];
        byte[] topicBytes = new byte[topicBytesLen];
        System.arraycopy(data, 2, topicBytes, 0, topicBytesLen);
        byte[] retData = new byte[data.length - 2 - topicBytesLen];
        System.arraycopy(data, 2 + topicBytesLen, retData, 0, retData.length);
        ZeroMQRequestData ret = null;
        try {
            ret = new ZeroMQRequestData(new String(topicBytes, "UTF-8"), retData);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
