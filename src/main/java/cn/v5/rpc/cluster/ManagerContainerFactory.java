package cn.v5.rpc.cluster;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Created by fangliang on 1/4/16.
 */
public class ManagerContainerFactory {

    private static ManagerContainerFactory instance = new ManagerContainerFactory();

    private ConcurrentHashMap<String, MRConnectionManagerContainer> containerHolder = new ConcurrentHashMap<>();

    public static ManagerContainerFactory getInstance() {
        return instance;
    }

    private ManagerContainerFactory() {

    }

    public MRConnectionManagerContainer build(String group, String discoveryURL, Executor businessExecutor, String provider, ClusterType clusterType) {
        String key = key(discoveryURL, group, clusterType);
        return containerHolder.computeIfAbsent(key, s -> new MRConnectionManagerContainerImpl(group, discoveryURL, businessExecutor, provider));
    }

    public MRConnectionManagerContainer buildBiz(String group, String discoveryURL, Executor businessExecutor, ClusterType clusterType) {
        String key = key(discoveryURL, group, clusterType);
        return containerHolder.computeIfAbsent(key, s -> new BizConnectionManagerContainer(group, discoveryURL, businessExecutor));
    }

    private String key(String discoveryURL, String group, ClusterType clusterType) {
        return String.format("%s_%s_%s", discoveryURL, group, clusterType.name());
    }
}
