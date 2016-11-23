package cn.v5.rpc.zeromq;

import org.zeromq.ZMQ;

public class ZeroMQRouterDealerProxy {
    private ZMQ.Context context;
    private ZMQ.Socket router;
    private ZMQ.Socket dealer;

    private String routerUrl;
    private String dealerUrl;

    private ZeroMQRouterDealerProxy() {

    }

    public ZeroMQRouterDealerProxy(ZMQ.Context context, String routerUrl, String dealerUrl) {
        this.context = context;
        this.routerUrl = routerUrl;
        this.dealerUrl = dealerUrl;
    }

    public ZeroMQRouterDealerProxy(String routerUrl, String dealerUrl) {
        this(ZMQ.context(1), routerUrl, dealerUrl);
    }

    public void start() {
        router = context.socket(ZMQ.ROUTER);
        dealer = context.socket(ZMQ.DEALER);

        router.bind(routerUrl);
        dealer.bind(dealerUrl);

        Thread proxyThread = new Thread(() -> ZMQ.proxy(router, dealer, null));
        proxyThread.setDaemon(true);
        proxyThread.start();
    }

    public void stop() {
        router.close();
        dealer.close();
        context.term();
    }
}
