package cn.v5.rpc.cluster;

import cn.v5.mr.MRClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Created by fangliang on 29/2/16.
 */
public class MRClusterMapClientManager {

    private static final Logger logger = LoggerFactory.getLogger(MRClusterMapClientManager.class);

    private ConcurrentHashMap<String, MRClusterConnectionManagerImpl> connectionManagerHolder = new ConcurrentHashMap<>();
    private Executor executor;
    private String discoverURL;

    public MRClient find(String node) {
        MRClusterConnectionManagerImpl connectionManager = connectionManagerHolder.computeIfAbsent(node, n -> new MRClusterConnectionManagerImpl());
        try {
            connectionManager.init(discoverURL, node, executor, null, ClusterType.MrBizPub.getType());
            return connectionManager.getMRClient();
        } catch (Exception e) {
        }
        return null;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setDiscoverURL(String discoverURL) {
        this.discoverURL = discoverURL;
    }
}
