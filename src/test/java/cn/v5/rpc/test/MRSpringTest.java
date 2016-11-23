package cn.v5.rpc.test;

import cn.v5.mr.MRConnectionManager;
import cn.v5.mr.MRPublisher;
import cn.v5.mr.MRSubscriber;
import cn.v5.mr.MessageResultContext;
import cn.v5.rpc.ConcurrentRpcFuture;
import cn.v5.rpc.NotifyRun;
import cn.v5.rpc.RpcClient;
import cn.v5.rpc.RpcFuture;
import cn.v5.rpc.error.RemoteError;
import cn.v5.rpc.error.TransportError;
import cn.v5.rpc.message.Messages;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.codec.binary.Hex;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/config-test-mr-spring.xml")
public class MRSpringTest {
    private static final Logger logger = LoggerFactory.getLogger(MRSpringTest.class);

    @Autowired
    @Qualifier("mrConnectionManager")
    MRConnectionManager connectionManager;

    @Autowired
    @Qualifier("mrConnectionManager2")
    MRConnectionManager connectionManager2;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    @Qualifier("taskExecutor")
    Executor executor;

    @Autowired
    @Qualifier("rpcClient")
    RpcClient rpcClient;

    @Autowired
    @Qualifier("rpcHttpClient")
    RpcClient rpcHttpClient;

    @BeforeClass
    public static void beforeClass() {
    }

    @Before
    public void before() {
    }

    @After
    public void after() {
    }

    @Test
    public void baseTest() throws UnsupportedEncodingException, InterruptedException {
        String topic = "base_topic";
        connectionManager2.addListener(topic, (subscriber, messageId, data) -> {
            logger.debug("recv message id {}, content {}", messageId, data);
            Assert.assertNotNull(data);
            subscriber.ackOk(messageId);
        });

        MRPublisher publisher = connectionManager.getMRPublisher(0);
        publisher.setPriority( 1 );
        for (int i = 0; i < 10; i++) {
            boolean ret = publisher.syncPub(topic, "test".getBytes("UTF-8") );
            if (!ret) {
                logger.error("sync pub error");
            }
            Assert.assertTrue(ret);
        }

        for (int i = 0; i < 10; i++) {
            boolean ret = publisher.asyncPub(topic, "test async".getBytes("UTF-8"), (status, mid, bytes) -> {
                logger.debug("async pub status {}, mid {}", status, mid );
            }  );
            if (!ret) {
                logger.error("async pub error");
            }
            Assert.assertTrue(ret);
        }

        Thread.sleep(2000);
    }

    @Test
    public void mrRpcSingleThreadTest() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("mrInvokerProxyHelloService");
        Thread.sleep(1000);
        String out = helloService.hello("mr rpc");
        Assert.assertNotNull(out);
        Assert.assertTrue(out.length() > 0);
        System.out.println("\n\nsay hello with mr : " + out);

        long n = 10000;
        long stime = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            String out2 = helloService.hello("mr rpc");
            Assert.assertNotNull(out2);
            Assert.assertTrue(out2.length() > 0);
        }

        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;
        System.out.println("\n mr rpc speed " + String.format("%,.0f", sp) + "/s\n");

    }

    @Test
    public void mrRpcTest() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("mrInvokerProxyHelloService");
        String out = helloService.hello("mr rpc");
        Assert.assertNotNull(out);
        Assert.assertTrue(out.length() > 0);
        System.out.println("\n\nsay hello with mr : " + out);

        ExecutorService executorService = Executors.newFixedThreadPool(100);

        long n = 40000;
        long stime = System.currentTimeMillis();
        final CountDownLatch countDownLatch = new CountDownLatch((int) n);
        for (int j = 0; j < 20; j++) {
            executorService.execute(() -> {
                for (int i = 0; i < 2000; i++) {
                    String out2 = helloService.hello("mr rpc");
                    countDownLatch.countDown();
                    Assert.assertNotNull(out2);
                }
            });
        }
        countDownLatch.await();
        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;
        System.out.println("\n mr rpc speed " + String.format("%,.0f", sp) + "/s\n");

    }

    @Test
    public void rpcLocalTest() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("localInvokerProxyHelloService");
        String out = helloService.hello("local rpc");
        Assert.assertNotNull(out);
        Assert.assertTrue(out.length() > 0);
        System.out.println("\n\nsay hello with local : " + out);

        ExecutorService executorService = Executors.newFixedThreadPool(100);

        long n = 100000;
        long stime = System.currentTimeMillis();
        /*
        final CountDownLatch countDownLatch = new CountDownLatch((int) n);
        for (int j = 0; j < 20; j++) {
            executorService.execute(() -> {
                for (int i = 0; i < 5000; i++) {
                    String out2 = helloService.hello("local rpc");
                    countDownLatch.countDown();
                    Assert.assertNotNull(out2);
                }
            });
        }
        countDownLatch.await();
        */
        for (int i = 0; i < n; i++) {
            String out2 = helloService.hello("local rpc");
            Assert.assertNotNull(out2);
        }

        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;
        System.out.println("\nlocal rpc speed " + String.format("%,.0f", sp) + "/s\n");

        HelloService helloService2 = (HelloService) applicationContext.getBean("helloService");
        n = n * 1000;
        stime = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            String out2 = helloService2.hello("native rpc");
            Assert.assertNotNull(out2);
        }

        use = System.currentTimeMillis() - stime;
        sp = (double) n / (double) use * 1000.0;
        System.out.println("\nnative speed " + String.format("%,.0f", sp) + "/s\n");

    }

    @Test
    public void directTest() throws UnsupportedEncodingException, InterruptedException {
        String topic = "direct_topic";
        connectionManager.addListener(topic, (subscriber, messageId, data) -> {
            logger.debug("recv direct message id {}, content {}", messageId, data);
            subscriber.ackOk(messageId, data);
        });

        MRPublisher publisher = connectionManager2.getMRPublisher(0);
        for (int i = 0; i < 10; i++) {
            MessageResultContext mrc = publisher.syncPubDirect(topic, "test".getBytes("UTF-8") );
            if (mrc.getResult() != 0) {
                logger.error("sync pub direct error");
            }
        }

        Thread.sleep(1000);
    }

    @Test
    public void directSpeedTest() throws UnsupportedEncodingException, InterruptedException {
        String topic = "direct_speed_topic";
        connectionManager.addListener(topic, 2000, MRSubscriber::ackOk);

        MRPublisher publisher = connectionManager2.getMRPublisher(0);
        publisher.syncPubDirect(topic, "test".getBytes("UTF-8") );
        long n = 10000;
        long stime = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            MessageResultContext mrc = publisher.syncPubDirect(topic, "test".getBytes("UTF-8") );
            if (mrc.getResult() != 0) {
                logger.error("sync pub direct error");
            }
        }
        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;
        System.out.println("\n\n mr sync direct speed " + String.format("%,.0f", sp) + "\n");


        n = 80000;
        stime = System.currentTimeMillis();
        final CountDownLatch countDownLatch = new CountDownLatch((int) n);
        for (int i = 0; i < n; i++) {
            boolean ret = publisher.asyncPubDirect(topic, "test".getBytes("UTF-8"), (status, mid, bytes) -> {
                if (status != 0) {
                    logger.error("async pub direct error in cb");
                }
                countDownLatch.countDown();
            } );
            if (!ret) {
                logger.error("async pub direct error");
                i--;
            }
            while (i - (n - countDownLatch.getCount()) > 999) {
                Thread.sleep(1);
            }
        }
        countDownLatch.await();
        use = System.currentTimeMillis() - stime;
        sp = (double) n / (double) use * 1000.0;
        System.out.println("\n mr async direct speed " + String.format("%,.0f", sp) + "\n");

    }

    @Test
    public void msgPackRpcTest() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("msgPackHelloService");
        String out = helloService.hello("msg pack mr rpc");
        Assert.assertNotNull(out);
        Assert.assertTrue(out.length() > 0);
        System.out.println("\nsay hello with msg pack mr : " + out + ", bin=" + Hex.encodeHexString(out.getBytes()) + "\n");

        User user = helloService.getUser("ok");
        Assert.assertNotNull(user);
        Assert.assertTrue("ok".equals(user.getName()));
        System.out.println("get user : " + user);

        int i = helloService.add(1, 1);
        Assert.assertTrue(i == 2);
        System.out.println("add 1,1 : " + i);

        helloService.callVoid();
        helloService.callVoid2("aa void aa");

        user.setName("default user");
        helloService.addUser(user);
        user.setName("delay user");
        NotifyRun.delay(4, () -> helloService.addUser(user));

        NotifyRun.run(helloService::callVoid);

        Thread.sleep(5000);
    }

    @Test
    public void msgPackRpcAsyncTest() throws InterruptedException, ExecutionException {
        HelloServiceAsync helloService = (HelloServiceAsync) applicationContext.getBean("msgPackHelloServiceAsync");

        MDC.put(Messages.TRACE_ID, "TRID-001");

        RpcFuture<String> outf = helloService.hello("msg pack mr rpc");
        outf.then(() -> {
            String out = null;
            try {
                out = outf.get();
                Assert.assertNotNull(out);
                Assert.assertTrue(out.length() > 0);
                logger.info("\nsay hello with msg pack mr : " + out + "\n");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        MDC.put(Messages.TRACE_ID, "TRID-002");

        Future<User> user = helloService.getUser("ok");
        Assert.assertNotNull(user.get());
        Assert.assertTrue("ok".equals(user.get().getName()));
        logger.info("get user : " + user.get());

        RpcFuture<Integer> i = helloService.add(1, 1);
        Assert.assertTrue(i.get() == 2);
        logger.info("add 1,1 : " + i.get());

        RpcFuture<Void> v = helloService.callVoid();
        v.get();
        RpcFuture<Void> v2 = helloService.callVoid2("aa void aa");
        v2.get();

        MDC.put(Messages.TRACE_ID, "TRID-003");

        RpcFuture<User> user2 = helloService.getUser("ok2");
        RpcFuture<Integer> i2 = helloService.add(1, 10);

        new ConcurrentRpcFuture<>(user2, i2).then((u3, i3) -> {
            logger.info("get user : " + u3);
            logger.info("add 1,10 : " + i3);
        });

        Thread.sleep(100000);
    }

    @Test
    public void msgPackRpcAsyncTest2() throws InterruptedException, ExecutionException {
        HelloServiceAsync helloService = (HelloServiceAsync) applicationContext.getBean("msgPackHelloServiceAsync");

        System.out.println("\n\n");
        helloService.getUser("ok3").then(user1 -> System.out.println("get user 1 : " + user1));
        helloService.getUser("ok3").then(() -> System.out.println("get user 2 ok."));

        RpcFuture<User> ju1 = helloService.getUser("ok5");
        RpcFuture<User> ju2 = helloService.getUser("ok6");

        ju1.join(ju2).then((u1, u2) -> {
            Assert.assertNotNull(u1);
            Assert.assertNotNull(u2);
            Assert.assertTrue("ok5".equals(u1.getName()));
            Assert.assertTrue("ok6".equals(u2.getName()));
            System.out.println("get user 3 : " + u1);
            System.out.println("get user 4 : " + u2);
        });

        RpcFuture<Void> ev = helloService.exception();
        ev.onError(e -> {
            Assert.assertNotNull(e);
            System.out.println("exxxxxxx 0: " + e.getMessage());
        }).then(() -> {
            System.out.println("okkkkkkkk");
        });

        RpcFuture<User> ju3 = helloService.getUser("ok5");
        RpcFuture<Void> ev2 = helloService.exception();
        ju3.join(ev2).then((u1, v) -> {
            System.out.println("okkkkkkkk 22");
        }).onError((u1, e1, u2, e2) -> {
            Assert.assertNotNull(u1);
            Assert.assertTrue("ok5".equals(u1.getName()));
            System.out.println("get user 5 : " + u1);
            System.out.println("get user 6 : " + u2);
            System.out.println("exxxxxxx 1: " + (e1 != null ? e1.getMessage() : ""));
            System.out.println("exxxxxxx 2: " + (e2 != null ? e2.getMessage() : ""));
        });

        Thread.sleep(500);
    }

    @Test
    public void msgPackLocalRpcTest() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("msgLocalPackHelloService");
        String out = helloService.hello("msg pack local rpc");
        Assert.assertNotNull(out);
        Assert.assertTrue(out.length() > 0);
        System.out.println("\n\nsay hello with msg local : " + out);

        Thread.sleep(5000);

        System.out.println("\n\nstart speed test");
        long n = 400000;
        long stime = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            String out2 = helloService.hello("msg pack local rpc");
            Assert.assertTrue(out.equals(out2));
        }

        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;

        System.out.println("\nlocal rpc speed " + String.format("%,.0f", sp) + "/s\n");

        Thread.sleep(1000);
    }

    @Test
    public void msgPackRpcTest2() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("msgPackHelloService");
        String out = helloService.hello("msg pack mr rpc");
        Assert.assertNotNull(out);
        Assert.assertTrue(out.length() > 0);
        System.out.println("\n\nsay hello with msg mr : " + out);

        long n = 20000;
        long stime = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            String out2 = helloService.hello("msg pack mr rpc");
            Assert.assertTrue(out.equals(out2));
        }

        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;
        System.out.println("\nmsg mr rpc speed " + String.format("%,.0f", sp) + "/s\n");

        ExecutorService executorService = Executors.newFixedThreadPool(100);

        n = 40000;
        stime = System.currentTimeMillis();
        final CountDownLatch countDownLatch = new CountDownLatch((int) n);
        for (int j = 0; j < 50; j++) {
            executorService.execute(() -> {
                for (int i = 0; i < 800; i++) {
                    String out2 = helloService.hello("msg pack mr rpc");
                    countDownLatch.countDown();
                    Assert.assertTrue(out.equals(out2));
                }
            });
        }
        countDownLatch.await();
        use = System.currentTimeMillis() - stime;
        sp = (double) n / (double) use * 1000.0;
        System.out.println("\n mr rpc speed " + String.format("%,.0f", sp) + "/s\n");

    }

    @Test
    public void rpcCallTest() throws InterruptedException {
        String out = rpcClient.callApply(String.class, "msg/rpc/hello", "hello", "call rpc");
        Assert.assertNotNull(out);
        Assert.assertTrue(out.length() > 1);
        System.out.println("\n\n mr rpc call for String : " + out + "\n");
        String name = "rpc user name";
        User user = rpcClient.callApply(User.class, "msg/rpc/hello", "getUser", name);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getName());
        Assert.assertTrue(name.equals(user.getName()));
        System.out.println("\n mr rpc call for User : " + user + "\n");

        user.setAge(100);
        user.setName("set new name");

        rpcClient.callApply("msg/rpc/hello", "addUser", user);

        int intValue = rpcClient.callApply(int.class, "msg/rpc/hello", "add", 1, 1);
        Assert.assertTrue(intValue == 2);
        System.out.println("\n mr rpc call add : " + intValue + "\n");

        rpcClient.callApply("msg/rpc/hello", "callVoid2", "call rpc retuen void");

        rpcClient.callApply("msg/rpc/hello", "callVoid");

        rpcClient.notifyApply("msg/rpc/hello", "callVoid2", "call rpc retuen void");

        rpcClient.notifyApply("msg/rpc/hello", "callVoid");

        Thread.sleep(10000);
    }

    @Test
    public void rpcCallExcpeptionTest() {
        int intValue = rpcClient.callApply(int.class, "msg/rpc/hello", "add", 1, 1);
        Assert.assertTrue(intValue == 2);
        System.out.println("\n mr rpc call add : " + intValue + "\n");

        try {
            rpcClient.callApply("msg/rpc/hello", "exception");
        } catch (TransportError e) {
            logger.error("transport error : {}", e.getMessage());
        } catch (RemoteError e) {
            logger.error("remote error : {}", e.getMessage());
        }
    }

    @Test
    public void rpcCallOverloadingTest() {

        HelloService helloService = (HelloService) applicationContext.getBean("msgPackHelloService");
        //HelloService helloService = (HelloService) applicationContext.getBean("mrInvokerProxyHelloService");

        int a1 = rpcClient.callApply(Integer.class, "msg/rpc/hello", "overloading", 1);
        int b1 = rpcClient.callApply(Integer.class, "msg/rpc/hello", "overloading_string", "ab");
        int c1 = rpcClient.callApply(Integer.class, "msg/rpc/hello", "overloading_string_int", "abc", 1);

        logger.info("a1={}, b1={}, c1={}", a1, b1, c1);

        Assert.assertEquals(a1, 1);
        Assert.assertEquals(b1, 2);
        Assert.assertEquals(c1, 4);

        int a = helloService.overloading(10);
        Assert.assertTrue(10 == a);
        System.out.println("\n a : " + a + "\n");


        int b = helloService.overloading("ok");
        Assert.assertTrue(2 == b);
        System.out.println("\n a : " + b + "\n");


        int c = helloService.overloading("ok", 1);
        Assert.assertTrue(3 == c);
        System.out.println("\n a : " + c + "\n");

//        try {
//            System.out.println("\nssssssss:\n");
//            helloService.exception();
//            System.out.println("\neeeeeeeee:\n");
//        } catch (Exception e) {
//            System.out.println("\nexception xxxxxx :\n");
//            e.printStackTrace();
//        }
    }

    @Test
    public void rpcCallAnnoTest() throws InterruptedException {

        String name = "rpc user name";
        User user = rpcClient.callApply(User.class, "rpc/anno/service", "getUser", name);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getName());
        Assert.assertTrue(name.equals(user.getName()));
        System.out.println("\n mr rpc call for User : " + user + "\n");

        int i1 = rpcClient.callApply(int.class, "rpc/anno/service", "add", 1, 2);
        Assert.assertTrue(i1 == 3);
        System.out.println("\n mr rpc call add : " + i1 + "\n");

        int i2 = rpcClient.callApply(int.class, "rpc/anno/service", "add2", 2, 3);
        Assert.assertTrue(i2 == 5);
        System.out.println("\n mr rpc call add2 : " + i2 + "\n");

        user = rpcClient.callApply(User.class, "rpc/anno1", "getUser", name);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getName());
        Assert.assertTrue(name.equals(user.getName()));
        System.out.println("\n mr rpc call for User : " + user + "\n");

        i2 = rpcClient.callApply(int.class, "rpc/anno3", "addTwo", 2, 3);
        Assert.assertTrue(i2 == 5);
        System.out.println("\n mr rpc call addTwo : " + i2 + "\n");

    }

    @Test
    public void rpcCallAnnoDelayTest() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            rpcClient.delayNotifyApply(i + 2, "rpc/anno/service", "delay", i + 2, System.currentTimeMillis());
        }
        Thread.sleep(15000);
    }

    @Test
    public void rpcCallAnnoAsyncTest() throws InterruptedException {
        String name = "rpc user name";
        System.out.println("\n\nthead:" + Thread.currentThread());
        RpcFuture<User> fu = rpcClient.callAsyncApply(User.class, "rpc/anno/service", "getUser", name);
        fu.then(u2 -> {
            User user = u2;
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getName());
            Assert.assertTrue(name.equals(user.getName()));
            System.out.println("\n mr rpc call for User : " + user + "\n");
            System.out.println("\ncallback thead:" + Thread.currentThread());
        }).onError(Throwable::printStackTrace);

        Thread.sleep(2000);
    }

    @Test
    public void rpcCallAnnoAsyncSpeedTest() throws InterruptedException {
        String name = "rpc user name";
        System.out.println("\n\nthead:" + Thread.currentThread());
        RpcFuture<User> fu2 = rpcClient.callAsyncApply(User.class, "rpc/anno/service", "getUser", name);
        fu2.then(u2 -> {
            User user = u2;
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getName());
            Assert.assertTrue(name.equals(user.getName()));
            System.out.println("\n mr rpc call for User : " + user + "\n");
            System.out.println("\ncallback thead:" + Thread.currentThread());
        }).onError(Throwable::printStackTrace);

        int n = 40000;
        long stime = System.currentTimeMillis();
        final CountDownLatch countDownLatch = new CountDownLatch(n);
        for (int j = 0; j < n; j++) {
            RpcFuture<User> fu3 = rpcClient.callAsyncApply(User.class, "rpc/anno/service", "getUser", name);
            fu3.then(u3 -> {
                User user = u3;
                Assert.assertNotNull(user);
                countDownLatch.countDown();
            }).onError(Throwable::printStackTrace);
            while (j - (n - countDownLatch.getCount()) > 200) {
                Thread.sleep(1);
            }
        }
        countDownLatch.await();
        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;
        System.out.println("\n mr rpc speed " + String.format("%,.0f", sp) + "/s\n");

        Thread.sleep(2000);
    }

    @Test
    public void msgPackHttpRpcTest() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("msgPackHttpHelloService");
        String out = helloService.hello("msg pack mr http rpc");
        Assert.assertNotNull(out);
        Assert.assertTrue(out.length() > 0);
        System.out.println("\nsay hello with msg pack mr http : " + out + "\n");

        User user = helloService.getUser("ok");
        Assert.assertNotNull(user);
        Assert.assertTrue("ok".equals(user.getName()));
        System.out.println("get user : " + user);

        int i2 = helloService.add(1, 1);
        Assert.assertTrue(i2 == 2);
        System.out.println("add 1,1 : " + i2);

        helloService.callVoid();
        helloService.callVoid2("aa void aa");

        Thread.sleep(2000);
    }

    @Test
    public void msgPackHttpRpcSpeedTest() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("msgPackHttpHelloService");
        String out = helloService.hello("msg pack mr http rpc");
        Assert.assertNotNull(out);
        Assert.assertTrue(out.length() > 0);
        System.out.println("\nsay hello with msg pack mr http : " + out + "\n");


        User user = helloService.getUser("ok");
        Assert.assertNotNull(user);
        Assert.assertTrue("ok".equals(user.getName()));
        System.out.println("get user : " + user);

        int i2 = helloService.add(1, 1);
        Assert.assertTrue(i2 == 2);
        System.out.println("add 1,1 : " + i2);

        helloService.callVoid();
        helloService.callVoid2("aa void aa");

        int n = 10000;
        long stime = System.currentTimeMillis();
        for (int j = 0; j < n; j++) {
            String out2 = helloService.hello("msg pack mr http rpc");
            Assert.assertNotNull(out2);
        }
        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;
        System.out.println("\n mr http rpc speed " + String.format("%,.0f", sp) + "/s\n");

        ExecutorService executorService = Executors.newFixedThreadPool(100);

        n = 40000;
        stime = System.currentTimeMillis();
        final CountDownLatch countDownLatch3 = new CountDownLatch(n);
        for (int j = 0; j < 20; j++) {
            executorService.execute(() -> {
                for (int i = 0; i < 2000; i++) {
                    String out2 = helloService.hello("msg pack mr http rpc");
                    Assert.assertNotNull(out2);
                    countDownLatch3.countDown();
                }
            });
        }
        countDownLatch3.await();
        use = System.currentTimeMillis() - stime;
        sp = (double) n / (double) use * 1000.0;
        System.out.println("\n mr http rpc sync 20 thread  speed " + String.format("%,.0f", sp) + "/s\n");

        n = 30000;
        stime = System.currentTimeMillis();
        final CountDownLatch countDownLatch = new CountDownLatch(n);
        for (int j = 0; j < n; j++) {
            RpcFuture<User> fu3 = rpcHttpClient.callAsyncApply(User.class, "rpc/anno/service", "getUser", "ok");
            fu3.then(u2 -> {
                Assert.assertNotNull(u2);
                countDownLatch.countDown();
            }).onError(Throwable::printStackTrace);
            while (j - (n - countDownLatch.getCount()) > 200) {
                Thread.sleep(1);
            }
        }
        countDownLatch.await();
        use = System.currentTimeMillis() - stime;
        sp = (double) n / (double) use * 1000.0;
        System.out.println("\n mr http rpc async speed " + String.format("%,.0f", sp) + "/s\n");

        Thread.sleep(2000);
    }

    @Test
    public void mrHttpInvokerTest() throws InterruptedException, ExecutionException {
        String url = "http://localhost:8088/mr/api";
        String topic = "http_api_topic";

//        connectionManager.addListener(topic, (subscriber, messageId, data) -> {
//            //logger.debug("recv direct message id {}, content {}", messageId, data);
//            subscriber.ackOk(messageId);
//        });

        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        AsyncHttpClient.BoundRequestBuilder brb = asyncHttpClient.preparePost(url);
        brb.addFormParam("topic", "http_api_topic");
        brb.addFormParam("content", "http content");
        Future<Response> f = brb.execute();
        try {
            Response r = f.get();
            if (r.getStatusCode() == 200) {
                System.out.println("\n\nmr http publisher ok.");
            } else {
                System.out.println("\n\nmr http publisher failed. code " + r.getStatusCode());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        int n = 10000;
        long stime = System.currentTimeMillis();
        for (int j = 0; j < n; j++) {
            Future<Response> f2 = brb.execute();
            Response r = f2.get();
            Assert.assertNotNull(r);
            Assert.assertTrue(r.getStatusCode() == 200);
        }
        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;
        System.out.println("\n mr http pub speed " + String.format("%,.0f", sp) + "/s\n");

    }

    @Test
    public void mrHttpInterfaceTest() throws InterruptedException, ExecutionException, IOException {
        String url = "http://localhost:8088/mr/interface";
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();

        AsyncHttpClient.BoundRequestBuilder brb = asyncHttpClient.preparePost(url);
        Future<Response> f = brb.execute();
        Response r = f.get();
        Assert.assertTrue(200 == r.getStatusCode());
        System.out.println("\n\nrpc service topics:");
        System.out.print(r.getResponseBody("UTF-8"));

        AsyncHttpClient.BoundRequestBuilder brb2 = asyncHttpClient.preparePost(url);
        brb2.addFormParam("topic", "msg/rpc/hello");
        Future<Response> f2 = brb2.execute();
        Response r2 = f2.get();
        Assert.assertTrue(200 == r2.getStatusCode());
        System.out.println("\n\nmr http interface ok.");
        System.out.print(r2.getResponseBody("UTF-8"));

        AsyncHttpClient.BoundRequestBuilder brb3 = asyncHttpClient.preparePost(url);
        brb3.addFormParam("topic", "msg/rpc/hello");
        brb3.addFormParam("type", "javaAsync");
        Future<Response> f3 = brb3.execute();
        Response r3 = f3.get();
        if (r3.getStatusCode() == 200) {
            System.out.println("\n\nmr http interface ok.");
            System.out.print(r3.getResponseBody("UTF-8"));
        } else {
            System.out.println("\n\nmr http interface failed. code " + r3.getStatusCode());
        }

        Thread.sleep(1000);
    }

    @Test
    public void zeroMQRpcTest() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("zeroMQHelloService");
        String out = helloService.hello("msg pack zeromq rpc");
        Assert.assertNotNull(out);
        Assert.assertTrue(out.length() > 0);
        System.out.println("\nsay hello with msg pack mr : " + out + ", bin=" + Hex.encodeHexString(out.getBytes()) + "\n");

        int n = 20000;
        long stime = System.currentTimeMillis();
        for (int j = 0; j < n; j++) {
            String out2 = helloService.hello("msg pack zeromq rpc");
            Assert.assertNotNull(out2);
        }
        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;
        System.out.println("\n zeromq rpc speed " + String.format("%,.0f", sp) + "/s\n");


        ExecutorService executorService = Executors.newFixedThreadPool(100);

        n = 80000;
        stime = System.currentTimeMillis();
        final CountDownLatch countDownLatch = new CountDownLatch((int) n);
        for (int j = 0; j < 50; j++) {
            executorService.execute(() -> {
                for (int i = 0; i < 1600; i++) {
                    String out2 = helloService.hello("msg pack zeromq rpc");
                    Assert.assertNotNull(out2);
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await();
        use = System.currentTimeMillis() - stime;
        sp = (double) n / (double) use * 1000.0;
        System.out.println("\n zeromq speed " + String.format("%,.0f", sp) + "/s\n");

        Thread.sleep(1000);
    }

    @Test
    public void zeroMQRpcAsyncTest() throws InterruptedException, ExecutionException {
        HelloServiceAsync helloService = (HelloServiceAsync) applicationContext.getBean("zeroMQHelloServiceAsync");

        MDC.put(Messages.TRACE_ID, "TRID-001");

        for (int i = 0; i < 4; i++) {
            RpcFuture<String> outf = helloService.hello("msg pack zeromq rpc");
            outf.then(() -> {
                String out = null;
                try {
                    out = outf.get();
                    Assert.assertNotNull(out);
                    logger.info("say hello with msg pack zeromq : " + out + "\n");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        int n = 400000;
        long stime = System.currentTimeMillis();
        final CountDownLatch countDownLatch = new CountDownLatch(n);
        for (int j = 0; j < n; j++) {
            RpcFuture<String> outf2 = helloService.hello("msg pack zeromq rpc");
            outf2.then(out2 -> {
                Assert.assertNotNull(out2);
                countDownLatch.countDown();
            });
            while (j - (n - countDownLatch.getCount()) > 2000) {
                Thread.sleep(1);
            }
        }
        countDownLatch.await();
        long use = System.currentTimeMillis() - stime;
        double sp = (double) n / (double) use * 1000.0;
        System.out.println("\n zeromq speed " + String.format("%,.0f", sp) + "/s\n");
//        MDC.put(Messages.TRACE_ID, "TRID-002");
//
//        Future<User> user = helloService.getUser("ok");
//        Assert.assertNotNull(user.get());
//        Assert.assertTrue("ok".equals(user.get().getName()));
//        logger.info("get user : " + user.get());
//
//        RpcFuture<Integer> i = helloService.add(1, 1);
//        Assert.assertTrue(i.get() == 2);
//        logger.info("add 1,1 : " + i.get());
//
//        RpcFuture<Void> v = helloService.callVoid();
//        v.get();
//        RpcFuture<Void> v2 = helloService.callVoid2("aa void aa");
//        v2.get();
//
//        MDC.put(Messages.TRACE_ID, "TRID-003");
//
//        RpcFuture<User> user2 = helloService.getUser("ok2");
//        RpcFuture<Integer> i2 = helloService.add(1, 10);
//
//        new ConcurrentRpcFuture<>(user2, i2).then((u3, i3) -> {
//            logger.info("get user : " + u3);
//            logger.info("add 1,10 : " + i3);
//        });

        Thread.sleep(5000);
    }

    @Test
    public void zeroMQRpcTestListAndMap() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("zeroMQHelloService");
        User user = new User("ccccc", "email", 10, 1, 1.1);
        List<User> users = new ArrayList<>();
        users.add(new User("name0", "email", 10, 1, 1.1));
        users.add(new User("name1", "email", 10, 1, 1.1));
        users.add(new User("name2", "email", 10, 1, 1.1));
        users.add(new User("name3", "email", 10, 1, 1.1));
        users.add(new User("name4", "email", 10, 1, 1.1));

        Address address = new Address("us", "ca", 10);
        user.setAddress(address);

        users.forEach(u -> u.setAddress(address));

        Assert.assertTrue(helloService.addUser(user));
        Assert.assertTrue(helloService.addUsers(users));

        User u = helloService.getUser("hi");
        Assert.assertNotNull(u);
        Assert.assertEquals(u.getName(), "hi");

        List<User> us = helloService.getUsers();
        Assert.assertNotNull(us);
        Assert.assertTrue(us.size() == 5);
        us.forEach(Assert::assertNotNull);

        Map<String, User> map = new HashMap<>();
        map.put("name0", new User("name0", "email", 10, 1, 1.1));
        map.put("name1", new User("name1", "email", 10, 1, 1.1));
        map.put("name2", new User("name2", "email", 10, 1, 1.1));
        map.put("name3", new User("name3", "email", 10, 1, 1.1));

        map.values().forEach(u1 -> u1.setAddress(address));

        Map<String, User> map2 = helloService.addMapUsers(map);
        Assert.assertNotNull(map2);
        Assert.assertEquals(map2.size(), 4);

        Assert.assertEquals(helloService.overloading(1), 1);
        Assert.assertEquals(helloService.overloading("ab"), 2);
        Assert.assertEquals(helloService.overloading("abc", 1), 4);
    }

    @Test
    public void zeroMQRpcTestBytes() throws InterruptedException {
        HelloService helloService = (HelloService) applicationContext.getBean("zeroMQHelloService");
        byte[] bytes = new byte[]{0, 1, 2, 3};
        byte[] out = helloService.getBytes(bytes);
        Assert.assertNotNull(out);
        Assert.assertArrayEquals(out, bytes);

        int[] ints = new int[]{1, 2, 3};
        int[] intsOut = helloService.getInts(ints);
        Assert.assertNotNull(intsOut);
        Assert.assertArrayEquals(intsOut, ints);

        User[] users = new User[]{new User("name0", "email", 10, 1, 1.1), new User("name1", "email", 10, 1, 1.1)};
        User[] usersOut = helloService.getUsers2(users);
        Assert.assertNotNull(usersOut);
        Assert.assertTrue(users.length == usersOut.length);
        for (int i=0; i<users.length; i++){
            User u1 = users[i];
            User u2 = usersOut[i];
            Assert.assertNotNull(u1);
            Assert.assertNotNull(u2);
            Assert.assertEquals(u1.getName(), u2.getName());
        }

    }
}
