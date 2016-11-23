package cn.v5.rpc.cluster;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by fangliang on 19/2/16.
 */
public class RoundRobinLoadStrategy<T> implements LoadStrategy<T> {

    private final AtomicInteger current = new AtomicInteger();

    @Override
    public T get(List<T> list) {

        if (CollectionUtils.isEmpty(list)) {
            return null;
        }

        int next;
        int prev;

        do {
            prev = current.get();
            next = prev + 1;
            if (next >= list.size()) {
                next = 0;
            }
        } while (!current.compareAndSet(prev, next));

        return list.get(next);

    }

    @Override
    public List<T> find(List<T> list) {
        
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }

        int next;
        int prev;

        do {
            prev = current.get();
            next = prev + 1;
            if (next >= list.size()) {
                next = 0;
            }
        } while (!current.compareAndSet(prev, next));


        List<T> result = new ArrayList<>();

        for (int i = next; i < next + list.size(); i++) {
            result.add(list.get(i % list.size()));
        }

        return result;
    }
}
