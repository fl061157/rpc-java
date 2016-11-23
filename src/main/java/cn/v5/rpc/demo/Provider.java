package cn.v5.rpc.demo;

import cn.v5.rpc.cluster.MRClusterConnectionManagerSpring;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Created by fangliang on 22/2/16.
 */
public class Provider {

    public static void main(String[] args) {
        String loading = "src/main/resources/config-test-mr-provider.xml";

        String provider = (args != null && args.length > 0) ? args[0] : "provider_cn_1";

        System.setProperty("rpc.provider", provider);

        ApplicationContext context = new FileSystemXmlApplicationContext(
                new String[]{loading});
        context.getBean(MRClusterConnectionManagerSpring.class);
    }


}
