package cn.v5.rpc.test;

import cn.v5.rpc.zeromq.ZeroMQRequestData;
import cn.v5.rpc.zeromq.ZeroMQUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(BlockJUnit4ClassRunner.class)
public class ZeroMQUtilsTest {
    private static final Logger logger = LoggerFactory.getLogger(ZeroMQUtilsTest.class);

    @Test
    public void test1() {
        byte[] bytes = new byte[]{1, 2, 3};
        String topic = "topictest";

        byte[] ret1 = ZeroMQUtils.getRequestData(topic, bytes);
        logger.debug("ret1 {}", ret1);

        ZeroMQRequestData requestData = ZeroMQUtils.getZeroMQRequestData(ret1);
        logger.debug("requestData {}", requestData);
        Assert.assertNotNull(requestData);
        Assert.assertNotNull(requestData.getTopic());
        Assert.assertNotNull(requestData.getData());
        Assert.assertEquals(topic, requestData.getTopic());
        Assert.assertArrayEquals(bytes, requestData.getData());
    }

}
