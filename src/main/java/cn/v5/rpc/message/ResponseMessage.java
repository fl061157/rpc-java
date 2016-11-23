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
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.msgpack.*;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;
import org.slf4j.MDC;

import java.io.IOException;

public class ResponseMessage implements MessagePackable {
    private int msgid;
    private Object error;
    private Object result;

    public ResponseMessage(int msgid, Object error, Object result) {
        this.msgid = msgid;
        this.error = error;
        this.result = result;
    }

    @Override
    public void writeTo(Packer pk) throws IOException {
        pk.writeArrayBegin(4);
        pk.write(Messages.RESPONSE);
        pk.write(msgid);
        pk.write(error);
        pk.write(result);
        pk.writeArrayEnd();
    }

    @Override
    public void readFrom(Unpacker u) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void messagePack(Packer pk) throws IOException {
        writeTo(pk);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);
    }
}
