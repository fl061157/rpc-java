package cn.v5.rpc.cluster;

import cn.v5.rpc.cluster.util.NodeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Created by fangliang on 27/2/16.
 */
public class BizConnectionManagerContainer extends AbstractConnectionManagerContainer implements MRConnectionManagerContainer {

    private static final Logger logger = LoggerFactory.getLogger(BizConnectionManagerContainer.class);

    protected static final String BASE_TCP_GROUP_PATH = "/dudu/service/tcp/";

    public BizConnectionManagerContainer(String group, String discoveryURL, Executor businessExecutor) {
        super(group, discoveryURL, businessExecutor);
    }

    @Override
    protected void doStart() throws Exception {
        String v = discoverTransport.subscribeData();
        if (StringUtils.isEmpty(v)) {
            logger.error("Path:{} Data Is Empty !", subscribePath());
            throw new Exception("Path Data is Empty !");
        }

        Set<String> urlSet = NodeUtil.parseNode(v);

        if (CollectionUtils.isNotEmpty(urlSet)) {
            urlSet.stream().forEach(url -> {
                if (logger.isInfoEnabled()) {
                    logger.info("Add Mr Node Url:{} ", url);
                }
                try {
                    add(url);
                } catch (Exception e) {
                }
            });
        } else {
            throw new Exception("URLSet is Empty !");
        }
    }

    @Override
    protected String subscribePath() {
        return String.format("%s%s", BASE_TCP_GROUP_PATH, group);
    }

    @Override
    public void register() {

    }

    @Override
    public void update(Map.Entry<String, String> entry) {
        Set<String> urlSet = NodeUtil.parseNode(entry.getValue());
        if (CollectionUtils.isNotEmpty(urlSet)) {
            refresh(urlSet);
        }
    }
}
