package cn.v5.rpc.support;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http.server.accesslog.AccessLogAppender;
import org.glassfish.grizzly.http.server.accesslog.AccessLogBuilder;
import org.glassfish.grizzly.http.server.accesslog.AccessLogProbe;
import org.glassfish.grizzly.http.server.accesslog.ApacheLogFormat;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.util.Set;

public class GrizzlyHttpServer implements InitializingBean, DisposableBean {
    private static Logger logger = LoggerFactory.getLogger(GrizzlyHttpServer.class);

    private HttpServer server;
    private int port;
    private Set<HttpRequestHandlerServlet> httpRequestHandlerServlets;
    private boolean accessLogEnable = false;

    public void setPort(int port) {
        this.port = port;
    }

    public void setHttpRequestHandlerServlets(Set<HttpRequestHandlerServlet> httpRequestHandlerServlets) {
        this.httpRequestHandlerServlets = httpRequestHandlerServlets;
    }

    public void setAccessLogEnable(boolean accessLogEnable) {
        this.accessLogEnable = accessLogEnable;
    }

    @Override
    public void destroy() throws Exception {
        server.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (port < 1) {
            port = 8083;
        }
        server = new HttpServer();

        if (accessLogEnable) {
            server.getServerConfiguration()
                    .getMonitoringConfig()
                    .getWebServerConfig()
                    .addProbes(new AccessLogProbe(new AccessLogAppenderImpl(), ApacheLogFormat.COMBINED, AccessLogProbe.DEFAULT_STATUS_THRESHOLD));
            //final AccessLogBuilder builder = new AccessLogBuilder("/tmp/access.log");
            //builder.instrument(server.getServerConfiguration());
        }

        NetworkListener listener = new NetworkListener("RPCServer", NetworkListener.DEFAULT_NETWORK_HOST, port);
        server.addListener(listener);

        WebappContext ctx = new WebappContext("ctx", "");

        if (httpRequestHandlerServlets != null && httpRequestHandlerServlets.size() > 0) {
            for (HttpRequestHandlerServlet servlet : httpRequestHandlerServlets) {
                if (logger.isInfoEnabled()) {
                    logger.info("add Servlet '{}'", servlet.getName());
                }
                ServletRegistration servletRegistration = ctx.addServlet(servlet.getName(), servlet);
                servlet.getMappings().forEach(servletRegistration::addMapping);
            }
        } else {
            logger.warn("httpRequestHandlerServlets is null.");
        }

        ctx.deploy(server);

//        server.getServerConfiguration().addHttpHandler(
//                new StaticHttpHandler(GrizzlyHttpServer.class.getClassLoader().getResource("templates/static").getPath()), "/static");

        server.start();
    }

    private static Logger accessLogger = LoggerFactory.getLogger("HTTP-ACCESS");

    private class AccessLogAppenderImpl implements AccessLogAppender {

        @Override
        public void append(String accessLogEntry) throws IOException {
            accessLogger.info(accessLogEntry);
        }

        @Override
        public void close() throws IOException {

        }
    }
}
