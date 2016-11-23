package cn.v5.rpc.support;

import org.glassfish.grizzly.utils.ArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HttpRequestHandlerServlet extends HttpServlet {
    private static Logger logger = LoggerFactory.getLogger(HttpRequestHandlerServlet.class);

    private HttpRequestHandler httpRequestHandler;
    private String name;
    private ArraySet<String> mappings = new ArraySet<>(String.class);

    public void setHttpRequestHandler(HttpRequestHandler httpRequestHandler) {
        this.httpRequestHandler = httpRequestHandler;
    }

    public void setMapping(String... urlPatterns){
        mappings.addAll(urlPatterns);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArraySet<String> getMappings() {
        return mappings;
    }

    @Override
    public void init() throws ServletException {
        if (logger.isDebugEnabled()) {
            logger.debug("HttpRequestHandlerServlet init. name {}, handler {}", name, httpRequestHandler);
        }
    }


    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocaleContextHolder.setLocale(request.getLocale());
        try {
            httpRequestHandler.handleRequest(request, response);
        } catch (HttpRequestMethodNotSupportedException ex) {
            String[] supportedMethods = ex.getSupportedMethods();
            if (supportedMethods != null) {
                response.setHeader("Allow", StringUtils.arrayToDelimitedString(supportedMethods, ", "));
            }
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, ex.getMessage());
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
