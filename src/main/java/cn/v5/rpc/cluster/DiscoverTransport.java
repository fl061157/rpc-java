package cn.v5.rpc.cluster;

import java.util.List;
import java.util.Map;

/**
 * Created by fangliang on 16/2/16.
 */
public interface DiscoverTransport {

    public List<byte[]> findData();

    public Map<String, String> findPD();

    public String getData(String path);

    public void subscribeChild();

    public String subscribeData();

    public boolean create(String path, byte[] data);

    public boolean set(String path, byte[] data);

    public boolean exists(String path);

    public String get(String path);

    public boolean delete(String path);


}
