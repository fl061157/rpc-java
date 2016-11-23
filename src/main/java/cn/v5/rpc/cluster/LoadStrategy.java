package cn.v5.rpc.cluster;

import java.util.List;

/**
 * Created by fangliang on 19/2/16.
 */
public interface LoadStrategy<T> {
    public T get(List<T> list);
    public List<T> find(List<T> list);
}
