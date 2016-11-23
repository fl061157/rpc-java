package cn.v5.rpc.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/grizzly-http-server-test.xml")
public class GrizzlyHttpServerTest {
    private static final Logger logger = LoggerFactory.getLogger(GrizzlyHttpServerTest.class);

    @Test
    public void serverTest() throws InterruptedException {

        logger.debug("start.....");

        int n = 0;
        while (n++ < 150) {
            Thread.sleep(1000);
        }

        logger.debug("end......");
    }

}
