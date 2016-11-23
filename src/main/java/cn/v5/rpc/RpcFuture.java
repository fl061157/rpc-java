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

import cn.v5.rpc.error.RemoteError;
import cn.v5.rpc.message.Messages;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class RpcFuture<V> implements java.util.concurrent.Future<V> {
    private static final Logger logger = LoggerFactory.getLogger(RpcFuture.class);

    private RpcFutureAdapter impl;
    private Template resultTemplate;
    private MessagePack messagePack;
    private List<Consumer<Exception>> errorConsumers = new ArrayList<>();
    private AtomicBoolean errorConsumerDone = new AtomicBoolean(false);
    private RemoteError remoteError;

    public RpcFuture() {

    }

    RpcFuture(MessagePack messagePack, RpcFutureAdapter impl) {
        this(messagePack, impl, (Template) null);
    }

    RpcFuture(MessagePack messagePack, RpcFutureAdapter impl, Template resultTemplate) {
        this.impl = impl;
        this.resultTemplate = resultTemplate;
        this.messagePack = messagePack;
        this.impl.setTraceId(MDC.get(Messages.TRACE_ID));
    }

    public RpcFuture(MessagePack messagePack, RpcFutureAdapter f, Class<V> resultClass) {
        this(messagePack, f, (Template) null);
        if (resultClass != void.class && resultClass != Value.class) {
            this.resultTemplate = messagePack.lookup(resultClass);
        }
    }

    public RpcFuture<V> then(Consumer<V> consumer) {
        impl.attachCallback(() -> {
            MDC.put(Messages.TRACE_ID, impl.getTraceId());
            try {
                checkThrowError();
                consumer.accept(getResult());
            } finally {
                MDC.remove(Messages.TRACE_ID);
            }
        });
        return this;
    }

    public RpcFuture<V> then(Runnable runnable) {
        impl.attachCallback(() -> {
            MDC.put(Messages.TRACE_ID, impl.getTraceId());
            try {
                checkThrowError();
                runnable.run();
            } finally {
                MDC.remove(Messages.TRACE_ID);
            }
        });
        return this;
    }

    public RpcFuture<V> onError(Consumer<Exception> consumer) {
        errorConsumers.add(consumer);
        if (!impl.hasAttachCallback()) {
            impl.attachCallback(this::checkThrowError);
        } else if (impl.isDone() && checkError()) {
            checkErrorConsumer();
        }
        return this;
    }

    public <T> ConcurrentRpcFuture<V, T> join(RpcFuture<T> rpcFuture2) {
        return new ConcurrentRpcFuture<>(this, rpcFuture2);
    }

    @Override
    public V get() throws InterruptedException {
        join();
        checkThrowError();
        return getResult();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException,
            TimeoutException {
        join(timeout, unit);
        checkThrowError();
        return getResult();
    }

    public void join() throws InterruptedException {
        impl.join();
    }

    public void join(long timeout, TimeUnit unit) throws InterruptedException,
            TimeoutException {
        impl.join(timeout, unit);
    }

    @Override
    public boolean isDone() {
        return impl.isDone();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // FIXME
        return false;
    }

    @Override
    public boolean isCancelled() {
        // FIXME
        return false;
    }

    public V getResult() {
        Value result = impl.getResult();
        if (resultTemplate == null) {
            return (V) result;
        } else if (result.isNilValue()) {
            return null;
        } else {
            try {
                return (V) resultTemplate.read(
                        new Converter(messagePack, result), null);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }
    }

    public Value getError() {
        return impl.getError();
    }

    private boolean checkError() {
        if (remoteError != null) {
            return true;
        }

        if (!getError().isNilValue()) {
            remoteError = new RemoteError(getError());
            return true;
        }

        return false;
    }

    public void checkThrowError() {
        if (checkError()) {
            checkErrorConsumer();
            throw remoteError;
        }
    }

    private void checkErrorConsumer() {
        if (errorConsumers != null && remoteError != null && errorConsumerDone.compareAndSet(false, true)) {
            MDC.put(Messages.TRACE_ID, impl.getTraceId());
            try {
                errorConsumers.stream().forEach(consumer -> consumer.accept(remoteError));
            } finally {
                MDC.remove(Messages.TRACE_ID);
            }
        }
    }
}
