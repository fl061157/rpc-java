package cn.v5.rpc.cluster;

/**
 * Created by fangliang on 27/2/16.
 */
public enum ClusterType {
    RpcClient("rpcClient"),
    RpcServer("rpcServer"),
    MrBizSub("mrBizSub"),
    MrBizPub("mrBizPub");

    private String type;

    private ClusterType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ClusterType getClusterType(String type) {
        ClusterType[] cts = ClusterType.values();
        for (ClusterType ct : cts) {
            if (ct.getType().toLowerCase().equals(type.toLowerCase())) {
                return ct;
            }
        }
        return null;
    }
}
