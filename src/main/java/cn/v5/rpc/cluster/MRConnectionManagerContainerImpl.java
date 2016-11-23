package cn.v5.rpc.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Created by fangliang on 16/2/16.
 */
public class MRConnectionManagerContainerImpl extends AbstractConnectionManagerContainer implements MRConnectionManagerContainer {

    private static final Logger logger = LoggerFactory.getLogger(MRConnectionManagerContainerImpl.class);

    private final String provider;

    public MRConnectionManagerContainerImpl(String group, String discoveryURL, Executor businessExecutor, String provider) {
        super(group, discoveryURL, businessExecutor);
        this.provider = provider;
    }

    @Override
    protected void doStart() {
        discoverTransport.subscribeChild();
        Map<String, String> pD = discoverTransport.findPD();
        if (pD == null || pD.size() == 0) {
            logger.error("Group Path :{} Is Null !", group);
            throw new RuntimeException(String.format("Group Path : %s Is Null !", group));
        }

        pD.entrySet().stream().forEach(entry -> {
            try {
                add(entry);
            } catch (Exception e) {
                logger.error("", e);
            }
        });
    }

    @Override
    protected String subscribePath() {
        return MRConnectionManagerContainer.groupPath(group);
    }

    public void register() {
    }

    @Override
    public void update(Map.Entry<String, String> entry) {
        if (logger.isInfoEnabled()) {
            logger.info("No Need Update ! ");
        }
    }
}
