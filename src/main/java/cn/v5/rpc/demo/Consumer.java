package cn.v5.rpc.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by fangliang on 22/2/16.
 */
public class Consumer {

    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    static AtomicInteger count = new AtomicInteger() ;

    public static void main(String[] args) {

        String loading = "src/main/resources/config-test-mr-consumer.xml";
        ApplicationContext context = new FileSystemXmlApplicationContext(
                new String[]{loading});

        HelloService helloService = (HelloService) context.getBean("msgPackHelloService");


        Thread t = new Thread(() -> {

            while (true) {

                String out = null;
                try {
                    out = helloService.hello("msg pack mr rpc" + count.getAndIncrement() );
                } catch (Exception e) {
                    logger.error("", e);
                }
                System.out.println("Receive : " + out);

                if (logger.isDebugEnabled()) {
                    logger.debug("Receive: {} ", out);
                }

                try {
                    Thread.sleep(5 * 100);
                } catch (InterruptedException e) {
                }

            }
        });

        t.setDaemon(true);

        t.start();
    }


}
