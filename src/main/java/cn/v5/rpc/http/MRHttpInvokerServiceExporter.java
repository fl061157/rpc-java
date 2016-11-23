package cn.v5.rpc.http;

import cn.v5.mr.MRSubscriber;
import cn.v5.rpc.MRMessagePackInvokerService;
import cn.v5.rpc.RpcServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MRHttpInvokerServiceExporter implements HttpRequestHandler {
    private static Logger logger = LoggerFactory.getLogger(MRHttpInvokerServiceExporter.class);

    private RpcServiceManager rpcServiceManager;

    private static int MAX_REQUEST_SIZE = 1024 * 1024;

    public void setRpcServiceManager(RpcServiceManager rpcServiceManager) {
        this.rpcServiceManager = rpcServiceManager;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!"POST".equals(request.getMethod())){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Accept POST Method.");
            return;
        }

        String topic = getTopic(request);
        //logger.debug("http rpc for topic : {}", topic);
        if (topic == null){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "topic is null.");
            return;
        }

        MRMessagePackInvokerService service = rpcServiceManager.getService(topic);
        if (service == null) {
            logger.warn("topic '{}' service nod found.", topic);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        byte[] request_data = readRequestData(request, response);
        if (request_data == null || request_data.length < 1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "request data is null.");
            return;
        }

        service.onMessage(new HttpMRSubscriberImpl(response), 0, request_data);

    }

    private byte[] readRequestData(HttpServletRequest request, HttpServletResponse response) throws IOException {
        int len = request.getContentLength();
        if (len < 1 || len > MAX_REQUEST_SIZE) {
            logger.error("rpc http request content length err : {}", len);
            return null;
        }
        byte[] data = new byte[len];
        ServletInputStream is = request.getInputStream();
        is.read(data);

        return data;
    }

    private String getTopic(HttpServletRequest request) {
        /*
        String servletPath = request.getServletPath();
        String contextPath = request.getContextPath();
        String method = request.getMethod();
        String pathTranslated = request.getPathTranslated();
        String qstring = request.getQueryString();
        String uri = request.getRequestURI();
        String url = request.getRequestURL().toString();
        */

        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/") && pathInfo.length() > 1){
            return pathInfo.substring(1);
        }

        return null;
    }

    static class HttpMRSubscriberImpl implements MRSubscriber {
        private HttpServletResponse response;

        public HttpMRSubscriberImpl(HttpServletResponse res) {
            response = res;
        }

        @Override
        public void ackOk(long messageId) {
            ackOk(messageId, null);
        }

        @Override
        public void ackOk(long messageId, byte[] data) {
            try {
                if (data != null && data.length > 0) {
                    ServletOutputStream os = response.getOutputStream();
                    os.write(data);
                    os.flush();
                } else {
                    response.getWriter().print("");
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }

        @Override
        public void ackFail(long messageId) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
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
