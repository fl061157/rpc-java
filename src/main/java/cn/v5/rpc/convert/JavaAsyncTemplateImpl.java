package cn.v5.rpc.convert;


import cn.v5.rpc.dispatcher.DispatcherDescription;

import java.lang.reflect.Type;

/**
 * Created by Danny on 16-01-03.
 */
public class JavaAsyncTemplateImpl extends JavaSyncTemplateImpl {

    protected void addImport(StringBuilder sb, DispatcherDescription dispatcherDescription){
        sb.append("import RpcFuture;\n");
    }

    protected String getReturnName(Type type){
        StringBuilder sb = new StringBuilder();
        String name = getAsyncName(type);

        sb.append("RpcFuture<").append(name).append(">");

        return sb.toString();
    }

    private String getAsyncName(Type type){
        if (Void.TYPE == type){
            return "Void";
        }
        if(Integer.TYPE == type){
            return "Integer";
        }
        if(Short.TYPE == type){
            return "Short";
        }
        if(Byte.TYPE == type){
            return "Byte";
        }
        if(Boolean.TYPE == type){
            return "Boolean";
        }
        if(Long.TYPE == type){
            return "Long";
        }
        if(Float.TYPE == type){
            return "Float";
        }
        if(Double.TYPE == type){
            return "Double";
        }
        return getRealName(type);
    }
}

