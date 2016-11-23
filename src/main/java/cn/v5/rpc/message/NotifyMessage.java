package cn.v5.rpc.message;

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

import org.apache.commons.lang3.StringUtils;
import org.msgpack.MessagePackable;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;
import org.slf4j.MDC;

import java.io.IOException;

public class NotifyMessage implements MessagePackable {
    private String topic;
    private String method;
    private Object[] args;

    public NotifyMessage(String topic, String method, Object[] args) {
        this.method = method;
        this.args = args;
        this.topic = topic;
    }

    @Override
    public void writeTo(Packer pk) throws IOException {
        String traceId = StringUtils.trimToNull(MDC.get(Messages.TRACE_ID));
        if (traceId == null) {
            pk.writeArrayBegin(3);
            pk.write(Messages.NOTIFY);
        } else {
            pk.writeArrayBegin(4);
            pk.write(Messages.NOTIFY_WITH_TRACEID);
            pk.write(traceId);
        }
        pk.write(method);
        pk.writeArrayBegin(args.length);
        for (Object arg : args) {
            pk.write(arg);
        }
        pk.writeArrayEnd();
        pk.writeArrayEnd();
    }

    @Override
    public void readFrom(Unpacker u) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void messagePack(Packer pk) throws IOException {
        writeTo(pk);
    }
}
