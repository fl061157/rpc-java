package cn.v5.rpc.cluster;

import cn.v5.mr.MRConnectionManager;
import cn.v5.mr.MRMessageListener;

import java.util.Map;

/**
 * Created by fangliang on 23/2/16.
 */
public interface MRConnectionManagerContainer {


    String MR_GROUP_PATH = "/zk/mr/";

    void start() throws Exception;

    void start(Map<String, MRMessageListener> messageListenerMap) throws Exception;

    void register();

    void add(Map.Entry<String, String> entry) throws Exception;

    void update(Map.Entry<String, String> entry);

    void addListener(String topic, MRMessageListener mrMessageListener);

    void remove(Map.Entry<String, String> entry);

    void refresh();

    void shutdown();

    void shutdownAndWait() throws InterruptedException;

    Map<String, MRConnectionManager> getMrConnectionManagerCache();

    static String groupPath(String group) {
        return String.format("%s%s", MR_GROUP_PATH, group);
    }

    void setPerfetchSize(int perfetchSize);


}
