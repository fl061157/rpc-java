package cn.v5.rpc.http;

import cn.v5.mr.MRPublisher;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
 * Params: topic,delay,content,bin,sort
 */
public class MRHttpPublisherProxy implements HttpRequestHandler {
    private static Logger logger = LoggerFactory.getLogger(MRHttpPublisherProxy.class);

    private MRPublisher mrPublisher;

    public void setMrPublisher(MRPublisher mrPublisher) {
        this.mrPublisher = mrPublisher;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String topic = StringUtils.trimToNull(request.getParameter("topic"));
        int delay = NumberUtils.toInt(request.getParameter("topic"));
        boolean isBin = BooleanUtils.toBoolean(request.getParameter("bin"));
        String sort = StringUtils.trimToNull(request.getParameter("sort"));


        if (topic == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "topic is null");
            return;
        }

        byte[] bytes;
        if (isBin) {
            String content = StringUtils.trimToNull(request.getParameter("topic"));
            if (content == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "content is null");
                return;
            }
            bytes = Base64.decodeBase64(content);
        } else {
            String content = request.getParameter("content");
            if (content == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "content is null");
                return;
            }
            bytes = content.getBytes("UTF-8");
        }
        if (bytes == null || bytes.length < 1){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "content is null");
            return;
        }

        boolean ret = false;
        if (sort != null) {
            ret = mrPublisher.syncPubSort(topic, bytes, sort);
        }else if(delay>0){
            ret = mrPublisher.syncPubDelay(topic, bytes, delay);
        }else{
            ret = mrPublisher.syncPub(topic, bytes);
        }
        if (ret){
            response.setStatus(HttpServletResponse.SC_OK);
        }else{
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }
}
