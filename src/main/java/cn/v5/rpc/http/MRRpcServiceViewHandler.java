package cn.v5.rpc.http;

import cn.v5.rpc.MRMessagePackInvokerService;
import cn.v5.rpc.RpcServiceManager;
import cn.v5.rpc.convert.JavaAsyncTemplateImpl;
import cn.v5.rpc.convert.JavaSyncTemplateImpl;
import cn.v5.rpc.convert.LangTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public class MRRpcServiceViewHandler implements HttpRequestHandler {

    private static Logger logger = LoggerFactory.getLogger(MRRpcServiceViewHandler.class);

    private RpcServiceManager rpcServiceManager;

    public void setRpcServiceManager(RpcServiceManager rpcServiceManager) {
        this.rpcServiceManager = rpcServiceManager;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String topic = StringUtils.trimToNull(request.getParameter("topic"));
        if (topic != null){
            topicSource(topic, request.getParameter("type"), response);
        }else{
            listService(response);
        }
    }

    private void listService(HttpServletResponse response) throws IOException {
        Set<String> topics = rpcServiceManager.getTopics();
        String ret = ToStringBuilder.reflectionToString(topics.toArray(new String[topics.size()]), ToStringStyle.JSON_STYLE);
        response.getWriter().print(ret);
    }

    private void topicSource(String topic, String type, HttpServletResponse response) throws IOException {
        MRMessagePackInvokerService service = rpcServiceManager.getMRMessagePackInvokerService(topic);
        if (service == null){
            logger.error("topic {} not found.", topic);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        LangTemplate lt;
        if ("javaAsync".equalsIgnoreCase(type)){
            lt = new JavaAsyncTemplateImpl();
        }else{
            lt = new JavaSyncTemplateImpl();
        }
        response.getWriter().print(lt.source(topic, service.getDispatcherDescription()));
    }
}
