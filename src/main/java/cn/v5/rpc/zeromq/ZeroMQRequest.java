package cn.v5.rpc.zeromq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.zeromq.ZMQ;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class ZeroMQRequest implements InitializingBean, DisposableBean {
    private static Logger logger = LoggerFactory.getLogger(ZeroMQRequest.class);

    private final ZMQ.Context context = ZMQ.context(1);
    private BlockingQueue<ZMQ.Socket> requests = new LinkedBlockingQueue<>();
    private Executor executor;
    private String requestUrl;
    private int requestCount;

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public void setRequestCount(int requestCount) {
        this.requestCount = requestCount;
    }

    private void init() {
        for (int i=0; i<requestCount; i++){
            ZMQ.Socket request = context.socket(ZMQ.REQ);
            request.connect(requestUrl);
            try {
                requests.put(request);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
        logger.info("zeromq client init ok.");
    }

    private ZMQ.Socket getRequest(){
        try {
            return requests.take();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public byte[] syncSend(byte[] data){
        return syncSend(getRequest(), data);
    }

    private byte[] syncSend(ZMQ.Socket request, byte[] data){
        try {
            request.send(data);
            byte[] ret = request.recv();
            return ret;
        }finally {
            try {
                requests.put(request);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void asyncSend(byte[] data, Consumer<byte[]> consumer){
        executor.execute(() -> consumer.accept(syncSend(data)));
    }

    @Override
    public void destroy() throws Exception {
        while (requests.size() > 0){
            ZMQ.Socket request = requests.take();
            request.close();
        }
        context.term();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (executor == null) {
            throw new IllegalArgumentException("Property 'executor' is required");
        }

        if (requestUrl == null) {
            throw new IllegalArgumentException("Property 'requestUrl' is required");
        }

        if (requestCount < 1 || requestCount > 100){
            requestCount = 20;
        }

        init();
    }

}
