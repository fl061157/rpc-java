package cn.v5.rpc.cluster.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by fangliang on 2/3/16.
 */
public class NodeUtil {

    private final static String MR_TAG = "mr";

    private final static String ADDR_TAG = "addr";

    private static final Logger logger = LoggerFactory.getLogger(NodeUtil.class);

    public static Set<String> parseNode(String content) {

        Set<String> result = null;
        if (StringUtils.isEmpty(content)) {
            return result;
        }
        TcpChannelInfoArray tcpChannelInfoArray;
        try {
            tcpChannelInfoArray = JSON.parseObject(content, TcpChannelInfoArray.class);
        } catch (Exception e) {
            logger.error("TcpChannelInfo Format Error Content:{} ", content);
            return result;
        }
        result = new HashSet<>();
        for (TcpChannelInfo tcpChannelInfo : tcpChannelInfoArray.getMq()) {
            if (tcpChannelInfo.getType().equals(MR_TAG)) {
                String info = tcpChannelInfo.getInfo();
                if (StringUtils.isEmpty(info)) {
                    logger.error("TcpChannelInfo Data Empty !");
                    continue;
                }
                try {
                    Map<String, Object>[] mapArray = JSON.parseObject(info, Map[].class);

                    for (Map<String, Object> map : mapArray) {
                        Object addr;
                        if ((addr = map.get(ADDR_TAG)) != null) {
                            result.add((String) addr);
                        }
                    }
                } catch (Exception e) {
                    logger.error("TcpChannelInfo Data Format Error info:{}", info);
                }
            }
        }
        return result;
    }

    public static class TcpChannelInfo implements Serializable {

        private String type;
        private String info;

        public String getInfo() {
            return info;
        }

        public String getType() {
            return type;
        }

        public void setInfo(String info) {
            this.info = info;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class TcpChannelInfoArray implements Serializable {

        @JSONField
        private TcpChannelInfo[] mq;

        public TcpChannelInfo[] getMq() {
            return mq;
        }

        public void setMq(TcpChannelInfo[] mq) {
            this.mq = mq;
        }
    }


}
