package cn.v5.rpc.zeromq;

import cn.v5.mr.MRSubscriber;
import cn.v5.rpc.MRMessagePackInvokerService;
import cn.v5.rpc.RpcServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import zmq.ZError;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class ZeroMQInvokerServiceExporter implements InitializingBean, DisposableBean {
    private static Logger logger = LoggerFactory.getLogger(ZeroMQInvokerServiceExporter.class);

    private RpcServiceManager rpcServiceManager;
    private Executor executor;

    private final ZMQ.Context context = ZMQ.context(1);
    private ZeroMQRouterDealerProxy proxy;

    private String routerUrl;
    private String dealerUrl;

    private int responseCount;

    public void setRpcServiceManager(RpcServiceManager rpcServiceManager) {
        this.rpcServiceManager = rpcServiceManager;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setRouterUrl(String routerUrl) {
        this.routerUrl = routerUrl;
    }

    public void setDealerUrl(String dealerUrl) {
        this.dealerUrl = dealerUrl;
    }

    public void setResponseCount(int responseCount) {
        this.responseCount = responseCount;
    }

    protected void handleRequest(byte[] data, Consumer<byte[]> consumer) {
        try {
            ZeroMQRequestData requestData = ZeroMQUtils.getZeroMQRequestData(data);
            String topic = requestData.getTopic();
            MRMessagePackInvokerService service = rpcServiceManager.getService(topic);
            if (service == null) {
                logger.warn("topic '{}' service nod found.", topic);
                return;
            }

            service.onMessage(new ZeroMQSubscriberImpl(consumer), 0, requestData.getData());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            consumer.accept(ZeroMQSubscriberImpl.errorBytes);
        }
    }

    private void startResponsePoller() {
        int num = responseCount;

        ZMQ.Poller poller = new ZMQ.Poller(num);
        ZMQ.PollItem items[] = new ZMQ.PollItem[num];
        BlockingQueue<byte[]>[] resQueues = new BlockingQueue[num];
        for (int i = 0; i < num; i++) {
            ZMQ.Socket response = context.socket(ZMQ.REP);
            response.connect(dealerUrl);
            items[i] = new ZMQ.PollItem(response, ZMQ.Poller.POLLIN | ZMQ.Poller.POLLOUT);
            resQueues[i] = new LinkedBlockingQueue<>();
            poller.register(items[i]);
        }

        Thread pollerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                int n = poller.poll();
                if (n < 0) {
                    break;
                }
                for (int i = 0; i < num; i++) {
                    ZMQ.PollItem item = items[i];
                    final BlockingQueue<byte[]> queue = resQueues[i];
                    if (item.isReadable()) {
                        ZMQ.Socket response = item.getSocket();
                        byte[] data = response.recv();
                        executor.execute(() -> handleRequest(data, ret -> {
                            try {
                                queue.put(ret);
                            } catch (InterruptedException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }));
                    }

                    if (item.isWritable()) {
                        ZMQ.Socket response = item.getSocket();
                        do {
                            byte[] data = queue.poll();
                            if (data == null) {
                                break;
                            }
                            response.send(data);
                        } while (true);
                    }
                }
            }
            logger.info("poller thread end.");
        });
        pollerThread.setDaemon(true);
        pollerThread.start();
    }

    private void startResponseThreads() {
        int num = responseCount;
        for (int i = 0; i < num; i++) {
            Thread t = new Thread(() -> {
                ZMQ.Socket response = context.socket(ZMQ.REP);
                response.connect(dealerUrl);

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        handleRequest(response.recv(), response::send);
                    } catch (ZMQException e) {
                        if (ZError.ETERM == e.getErrorCode()) {
                            break;
                        } else {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                response.close();
            });
            t.setDaemon(true);
            t.start();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        if (dealerUrl == null) {
            throw new IllegalArgumentException("Property 'dealerUrl' is required");
        }

        if (executor == null) {
            throw new IllegalArgumentException("Property 'executor' is required");
        }

        if (rpcServiceManager == null) {
            throw new IllegalArgumentException("Property 'rpcServiceManager' is required");
        }

        if (responseCount < 1 || responseCount > 200) {
            responseCount = 20;
        }

        if (routerUrl != null) {
            proxy = new ZeroMQRouterDealerProxy(context, routerUrl, dealerUrl);
            proxy.start();
        }

        startResponseThreads();
        //startResponsePoller();

        logger.info("zeromq response init ok.");
    }

    @Override
    public void destroy() throws Exception {
        if (proxy != null) {
            proxy.stop();
        } else {
            context.term();
        }
    }

    static class ZeroMQSubscriberImpl implements MRSubscriber {
        Consumer<byte[]> consumer;
        final static byte[] errorBytes = new byte[]{0};

        public ZeroMQSubscriberImpl(Consumer<byte[]> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void ackOk(long messageId) {
            ackFail(messageId);
        }

        @Override
        public void ackOk(long messageId, byte[] data) {
            consumer.accept(data);
        }

        @Override
        public void ackFail(long messageId) {
            consumer.accept(errorBytes);
        }

        @Override
        public void unSub() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void on(int status, long mid, byte[] bytes) {
            throw new UnsupportedOperationException();
        }
    }
}
