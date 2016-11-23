package cn.v5.rpc.http;

import cn.v5.mr.MRPublisher;
import cn.v5.mr.MessageAttribute;
import cn.v5.mr.MessageCallback;
import cn.v5.mr.MessageResultContext;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class MRHttpRpcPublisher implements MRPublisher, InitializingBean, DisposableBean {

    private static Logger logger = LoggerFactory.getLogger(MRHttpRpcPublisher.class);

    private String url;

    AsyncHttpClient asyncHttpClient;

    private ExecutorService executorService;

    @Override
    public void destroy() throws Exception {
        asyncHttpClient.close();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        if (executorService != null) {
            builder.setExecutorService(executorService);
        }
        asyncHttpClient = new AsyncHttpClient(builder.build());
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }


    @Override
    public void setPriority(int priority) {

    }

    public void setUrl(String url) {
        this.url = url;
    }

    private String getRequestUrl(String topic) {
        return url + "/" + topic;
    }


    @Override
    public boolean syncPub(String topic, byte[] data ) {
        MessageResultContext mrc = syncPubDirect(topic, data );
        return mrc.getResult() == 0;
    }

    @Override
    public boolean syncPub(String topic, byte[] data, MessageAttribute messageAttribute) {
        return syncPub(topic, data);
    }

    @Override
    public MessageResultContext syncPubForAck(String topic, byte[] data, MessageAttribute messageAttribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean syncPubDelay(String topic, byte[] data, int sec ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean syncPubSort(String topic, byte[] data, String sortKey ) {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean unsafePub(String s, byte[] bytes ) {
        return false;
    }

    @Override
    public MessageResultContext syncPubDirect(String topic, byte[] data ) {
        MessageResultContext mrc = new MessageResultContext();
        if (data != null && data.length > 0) {
            AsyncHttpClient.BoundRequestBuilder brb = asyncHttpClient.preparePost(getRequestUrl(topic));
            brb.setContentLength(data.length);
            brb.setBody(data);
            Future<Response> f = brb.execute();
            try {
                Response r = f.get();
                if (r.getStatusCode() == 200) {
                    byte[] bytes = r.getResponseBodyAsBytes();
                    mrc.setResult(0);
                    mrc.setBytes(bytes);
                } else {
                    logger.error("response error code {}", r.getStatusCode());
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            logger.error("pub data is null.");
        }
        return mrc;
    }

    @Override
    public Future<MessageResultContext> asyncPub(String topic, byte[] data ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean asyncPub(String topic, byte[] data, MessageCallback callback ) {
        return asyncPubDirect(topic, data, callback );
    }

    @Override
    public boolean asyncPub(String topic, byte[] data, MessageAttribute messageAttribute, MessageCallback callback ) {
        return asyncPubDirect(topic, data, callback );
    }

    @Override
    public boolean asyncPubDelay(String topic, byte[] data, int sec, MessageCallback callback ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean asyncPubSort(String topic, byte[] data, String sortKey, MessageCallback callback ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean asyncPubDirect(String topic, byte[] data, MessageCallback callback ) {
        boolean ret = false;
        if (data != null && data.length > 0) {
            AsyncHttpClient.BoundRequestBuilder brb = asyncHttpClient.preparePost(getRequestUrl(topic));
            brb.setContentLength(data.length);
            brb.setBody(data);
            brb.execute(new AsyncCompletionHandler() {
                @Override
                public Object onCompleted(Response r) throws Exception {
                    if (r.getStatusCode() == 200) {
                        byte[] bytes = r.getResponseBodyAsBytes();
                        callback.on(0, 0, bytes);
                    } else {
                        logger.error("response error code {}", r.getStatusCode());
                        callback.on(-1, 0, null);
                    }
                    return null;
                }

                @Override
                public void onThrowable(Throwable t) {
                    logger.error(t.getMessage(), t);
                    callback.on(-1, 0, null);
                }
            });
            ret = true;
        } else {
            logger.error("pub data is null.");
        }
        return ret;
    }


}
