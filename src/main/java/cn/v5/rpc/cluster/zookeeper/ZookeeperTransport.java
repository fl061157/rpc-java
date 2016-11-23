package cn.v5.rpc.cluster.zookeeper;

import cn.v5.rpc.cluster.DiscoverTransport;
import cn.v5.rpc.cluster.MRConnectionManagerContainer;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Created by fangliang on 16/2/16.
 */
public class ZookeeperTransport implements DiscoverTransport {

    private final String zkString;

    private final String path;

    private CuratorFramework client;

    private PathChildrenCache pathChildrenCache;

    private ConcurrentHashMap<String, NodeCache> nodeCacheMap = new ConcurrentHashMap<>();

    private AtomicBoolean start = new AtomicBoolean(false);

    private MRConnectionManagerContainer mrConnectionManagerContainer;

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperTransport.class);

    public ZookeeperTransport(String zkString, String path, MRConnectionManagerContainer mrConnectionManagerContainer) {
        this.zkString = zkString;
        this.path = path;
        this.mrConnectionManagerContainer = mrConnectionManagerContainer;
    }

    private void connect() {
        //TODO Config
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(60 * 1000, Integer.MAX_VALUE);
        client = CuratorFrameworkFactory.newClient(zkString, retryPolicy);
        client.start();
        if (logger.isDebugEnabled()) {
            logger.debug("Zookeeper Discover Start zkString:{} ", zkString);
        }
    }


    @Override
    public boolean create(String path, byte[] data) {
        if (start.get() && client != null) {
            try {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, data);
            } catch (Exception e) {
                logger.error("Create Zookeeper Path Error Path:{} , data:{} ", path, data, e);
            }
        }
        return false;
    }


    @Override
    public boolean set(String path, byte[] data) {
        if (start.get() && client != null) {
            try {
                client.setData().forPath(path, data);
            } catch (Exception e) {
                logger.error("Set Zookeeper Path Error Path:{} , Data:{}  ", path, new String(data), e);
            }
        }
        return false;
    }

    @Override
    public boolean delete(String path) {
        if (start.get() && client != null) {
            try {
                client.delete().forPath(path);
            } catch (Exception e) {
                logger.error("Delete Zookeeper Path Error Path:{} ", path, e);
            }
        }
        return false;
    }

    @Override
    public String get(String path) {
        if (start.get() && client != null) {
            try {
                return new String(client.getData().forPath(path));
            } catch (Exception e) {
                logger.error("Get Zookeeper Path Error Path:{} ", path, e);
            }
        }
        return null;
    }

    @Override
    public boolean exists(String path) {
        if (start.get() && client != null) {
            try {
                Stat stat = client.checkExists().forPath(path);
                return stat != null;
            } catch (Exception e) {
                logger.error("Create Zookeeper Path Error Path:{} ", path, e);
            }
        }
        return false;
    }


    private boolean verfiyPath(String path) {

        if (StringUtils.isBlank(path)) {
            logger.error("verfiy path is empty !");
            return false;
        }

        if (path.charAt(0) != '/') {
            logger.error("verfiy path must start with / character");
            return false;
        }

        if (path.charAt(path.length() - 1) == '/') {

            logger.error("verfiy path must not end with / character");
            return false;

        }

        return true;
    }


    @Override
    public String subscribeData() {
        if (start.compareAndSet(false, true)) {
            connect();
        }

        boolean vP = verfiyPath(path);

        if (!vP) {
            logger.error("verfiy path:{} error", path);
            return null;
        }


        NodeCache nodeCache = nodeCacheMap.computeIfAbsent(path, p -> {

            NodeCache nc = new NodeCache(client, p);

            try {
                nc.start(true);
            } catch (Exception e) {
                try {
                    nc.close();
                } catch (IOException ie) {
                    logger.error("close ", ie);
                }
                logger.error("NodeCache Of Path:{} Start False !", p);
                return null;
            }

            NodeCacheListener listener = () -> {

                String v;
                if (nc.getCurrentData() != null && StringUtils.isNotEmpty((v = new String(nc.getCurrentData().getData())))) {

                    try {
                        String k = nc.getCurrentData().getPath();
                        mrConnectionManagerContainer.update(new AbstractMap.SimpleEntry<>(k, v));
                    } catch (Exception e) {
                        logger.error("Update Error !", e);
                    }
                } else {
                    if (logger.isWarnEnabled()) {
                        logger.warn("NodeCacheListener NodeCache Path:{}  Data Empty !", p);
                    }
                }
            };

            nc.getListenable().addListener(listener);

            return nc;
        });


        if (nodeCache == null) {
            logger.error("NodeCache Init Error path:{} ", path);
            return null;
        }

        return nodeCache.getCurrentData() != null ? new String(nodeCache.getCurrentData().getData()) : null;
    }

    @Override
    public void subscribeChild() {

        if (start.compareAndSet(false, true)) {
            connect();
        }

        pathChildrenCache = new PathChildrenCache(client, path, true, false, Executors.newSingleThreadExecutor());
        PathChildrenCacheListener listener = (client1, event) -> {
            switch (event.getType()) {
                case CHILD_ADDED:
                    try {
                        mrConnectionManagerContainer.add(new AbstractMap.SimpleEntry<>(event.getData().getPath(),
                                new String(event.getData().getData())));
                    } catch (Exception e) {
                        logger.error("Child Added error !");
                    }
                    break;
                case CHILD_REMOVED:
                    if (logger.isInfoEnabled()) {
                        logger.info("Remove Path:{} ", event.getData().getPath());
                    }
                    try {
                        mrConnectionManagerContainer.remove(new AbstractMap.SimpleEntry<>(event.getData().getPath(),
                                new String(event.getData().getData())));
                    } catch (Exception e) {
                        logger.error("Child Removed error !");
                    }
                    break;
                case CHILD_UPDATED:
                    try {
                        mrConnectionManagerContainer.update(new AbstractMap.SimpleEntry<>(event.getData().getPath(),
                                new String(event.getData().getData())));
                    } catch (Exception e) {
                        logger.error("Child Updated error !");
                    }
                    break;
                case CONNECTION_RECONNECTED:
                    if (logger.isInfoEnabled()) {
                        logger.info("Zookeeper Reconnected !");
                    }
                    try {
                        mrConnectionManagerContainer.refresh();
                    } catch (Exception e) {
                        logger.error("Reconnected error !");
                    }
                    break;
                case CONNECTION_LOST:
                case CONNECTION_SUSPENDED:
                    break;
                default:
            }
        };

        pathChildrenCache.getListenable().addListener(listener);

        try {
            pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        } catch (Exception e) {
            logger.error("zookeeper watcher exception path:{}", path, e);
            throw new RuntimeException(String.format("Zookeeper watcher exception path:%s", path), e);
        }
    }

    @Override
    public List<byte[]> findData() {
        List<ChildData> childDataList = pathChildrenCache.getCurrentData();
        if (childDataList == null || childDataList.size() == 0) {
            return null;
        }
        return childDataList.stream().map(childData -> childData.getData()).collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findPD() {
        List<ChildData> childDataList = pathChildrenCache.getCurrentData();
        if (childDataList == null || childDataList.size() == 0) {
            return null;
        }

        Map<String, String> result = new HashMap<>();

        childDataList.stream().forEach(cd -> {
                    String path = cd.getPath();
                    String[] pA = path.split("/");
                    result.put(pA == null || pA.length == 0 ? path : pA[pA.length - 1], new String(cd.getData()));
                }
        );
        return result;

    }

    @Override
    public String getData(String path) {
        ChildData childData = pathChildrenCache.getCurrentData(path);
        return childData != null ? new String(childData.getData()) : null;
    }

}
