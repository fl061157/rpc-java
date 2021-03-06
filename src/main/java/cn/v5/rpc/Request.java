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

import cn.v5.rpc.message.MessageSendable;
import cn.v5.rpc.message.ResponseMessage;
import org.msgpack.type.Value;

public class Request implements Callback<Object> {
    private MessageSendable channel; // TODO #SF synchronized?
    private int msgid;
    private String method;
    private Value args;

    public Request(MessageSendable channel, int msgid, String method, Value args) {
        this.channel = channel;
        this.msgid = msgid;
        this.method = method;
        this.args = args;
    }

    public Request(String method, Value args) {
        this.channel = null;
        this.msgid = 0;
        this.method = method;
        this.args = args;
    }

    public String getMethodName() {
        return method;
    }

    public Value getArguments() {
        return args;
    }

    public int getMessageID() {
        return msgid;
    }

    @Override
    public void sendResult(Object result) {
        sendResponse(result, null);
    }

    public void sendError(Object error) {
        sendResponse(null, error);
    }

    @Override
    public void sendError(Object error, Object data) {
        sendResponse(data, error);
    }

    public void sendResponse(Object result, Object error) {
        if (channel == null) {
            return;
        }
        ResponseMessage msg = new ResponseMessage(msgid, error, result);
        channel.sendMessage(msg);
        channel = null;
    }
}
