package cn.v5.rpc.support;

import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceAdapter extends AbstractExecutorService {
    private final TaskExecutor taskExecutor;


    /**
     * Create a new ExecutorServiceAdapter, using the given target executor.
     * @param taskExecutor the target executor to delegate to
     */
    public ExecutorServiceAdapter(TaskExecutor taskExecutor) {
        Assert.notNull(taskExecutor, "TaskExecutor must not be null");
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute(Runnable task) {
        this.taskExecutor.execute(task);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

}
