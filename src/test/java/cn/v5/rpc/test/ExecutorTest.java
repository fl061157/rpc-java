package cn.v5.rpc.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

@RunWith(BlockJUnit4ClassRunner.class)
public class ExecutorTest {

    @Test
    public void e1() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(20);

        LongAdder longAdder = new LongAdder();

        int n = 4_000_000;
        long start = System.currentTimeMillis();
        new Thread(() -> {
            for (int i=0; i<n; i++){
                executorService.submit(longAdder::increment);
            }
        }).start();

        while (true){
            long count = longAdder.longValue();
            if (count < n){
                Thread.sleep(1);
                continue;
            }
            break;
        }
        long end = System.currentTimeMillis();
        double sp = (double) n / (double) (end - start) * 1000.0;
        System.out.println("\n speed " + String.format("%,.0f", sp) + "/s\n");
    }

    @Test
    public void e2() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(20);

        LongAdder longAdder = new LongAdder();

        Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

        int n = 12_000_000;
        long start = System.currentTimeMillis();
        new Thread(() -> {
            for (int i=0; i<n; i++){
                queue.offer(longAdder::increment);
            }
        }).start();

        for (int i=0; i<6; i++){
            new Thread(() -> {
                while (!Thread.interrupted()) {
                    Runnable run = queue.poll();
                    if (run == null) {
                        //Thread.yield();
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    run.run();
                }
            }).start();
        }

        while (true){
            long count = longAdder.longValue();
            if (count < n){
                Thread.sleep(1);
                continue;
            }
            break;
        }
        long end = System.currentTimeMillis();
        double sp = (double) n / (double) (end - start) * 1000.0;
        System.out.println("\n speed " + String.format("%,.0f", sp) + "/s\n");

        //Thread.sleep(100000);
    }

    @Test
    public void e3() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(20);

        LongAdder longAdder = new LongAdder();

        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

        int n = 12_000_000;
        long start = System.currentTimeMillis();
        new Thread(() -> {
            for (int i=0; i<n; i++){
                queue.offer(longAdder::increment);
            }
        }).start();

        for (int i=0; i<4; i++){
            new Thread(() -> {
                while (!Thread.interrupted()) {
                    Runnable run = null;
                    try {
                        run = queue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (run == null) {
                        continue;
                    }
                    run.run();
                }
            }).start();
        }

        while (true){
            long count = longAdder.longValue();
            if (count < n){
                Thread.sleep(1);
                continue;
            }
            break;
        }
        long end = System.currentTimeMillis();
        double sp = (double) n / (double) (end - start) * 1000.0;
        System.out.println("\n speed " + String.format("%,.0f", sp) + "/s\n");
    }

}
