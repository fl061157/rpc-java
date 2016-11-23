package cn.v5.rpc.zeromq;

import java.util.Arrays;

public class ZeroMQRequestData {
    private String topic;
    private byte[] data;

    public ZeroMQRequestData(String topic, byte[] data) {
        this.topic = topic;
        this.data = data;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ZeroMQRequestData{" +
                "topic='" + topic + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
