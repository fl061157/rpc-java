package cn.v5.rpc.cluster;

/**
 * Created by fangliang on 1/3/16.
 */
public interface MessageListener {
    void onMessage(byte[] data) throws Throwable;
}
