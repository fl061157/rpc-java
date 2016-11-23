package cn.v5.rpc;
//
// MessagePack-RPC for Java
//
// Copyright (C) 2010 FURUHASHI Sadayuki
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//

import org.msgpack.type.Value;
import org.msgpack.type.ValueFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class RpcFutureAdapterAsyncImpl extends AbstractRpcFutureAdapter {

    private final Object lock = new Object();
    private int timeout;

    RpcFutureAdapterAsyncImpl(int timeout, Executor executor) {
        this.timeout = timeout;
        this.executor = executor;
    }

    @Override
    public void attachCallback(Runnable callback) {
        boolean was_already_done;
        synchronized (lock) {
            was_already_done = done;
            if (!done) {
                this.callbacks.add(callback);
            }
        }
        if (was_already_done) {
            executor.execute(callback);
        }
    }

    @Override
    public void join() throws InterruptedException {
        synchronized (lock) {
            while (!done) {
                lock.wait();
            }
        }
    }

    @Override
    public void join(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        long end_time = System.currentTimeMillis() + unit.toMillis(timeout);
        boolean run_callback = false;
        synchronized (lock) {
            while (!done) {
                long timeout_remaining = end_time - System.currentTimeMillis();
                if (timeout_remaining <= 0) break;
                lock.wait(timeout_remaining);
            }
            if (!done) {
                this.error = ValueFactory.createRawValue("timedout");
                done = true;
                lock.notifyAll();
                run_callback = true;
            }
        }
        if (run_callback) {
            callbacks.forEach(cb -> executor.execute(cb));
        }
    }

    @Override
    public void setResult(Value result, Value error) {
        synchronized (lock) {
            if (done) {
                return;
            }
            this.result = result;
            this.error = error;
            this.done = true;
            lock.notifyAll();
        }
        callbacks.forEach(cb -> executor.execute(cb));
    }

    boolean stepTimeout() {
        if (timeout <= 0) {
            return true;
        } else {
            timeout--;
            return false;
        }
    }
}
