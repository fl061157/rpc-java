package cn.v5.rpc.convert;

import cn.v5.rpc.dispatcher.DispatcherDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

public class JavaSyncTemplateImpl extends AbstractLangTemplate {
    private static Logger logger = LoggerFactory.getLogger(JavaSyncTemplateImpl.class);

    @Override
    public String source(String topic, DispatcherDescription dispatcherDescription) {
        StringBuilder sb = new StringBuilder();
        javaStart(sb, topic, dispatcherDescription);

        for (DispatcherDescription.DispatcherMethod dd : dispatcherDescription.getDispatcherMethods()){
            addMethod(sb, dd);
        }

        javaEnd(sb, dispatcherDescription);
        return sb.toString();
    }

    protected void addImport(StringBuilder sb, DispatcherDescription dispatcherDescription){

    }

    protected void javaStart(StringBuilder sb, String topic, DispatcherDescription dispatcherDescription){
        String ifaceName = dispatcherDescription.getName();
        String className = null;
        String packageName = null;
        if (ifaceName != null && ifaceName.indexOf('.') > 0){
            int pos = ifaceName.lastIndexOf('.');
            packageName = ifaceName.substring(0, pos);
            className = ifaceName.substring(pos + 1);
        }else{
            packageName = null;
            className = ifaceName;
        }
        sb.append("\n\n");
        if (topic != null){
            sb.append("/* topic : ").append(topic).append(" */\n");
        }
        if (packageName != null){
            sb.append("package ").append(packageName).append(";\n");
        }
        addImport(sb, dispatcherDescription);
        sb.append("public interface ").append(className).append(" {\n");
    }

    protected void javaEnd(StringBuilder sb, DispatcherDescription dispatcherDescription){
        sb.append("}\n");
    }

    protected void addMethod(StringBuilder sb, DispatcherDescription.DispatcherMethod dispatcherMethod){
        String returnName = getReturnName(dispatcherMethod.getReturnType());
        String name = dispatcherMethod.getName();
        sb.append("    ").append(returnName).append(" ").append(name).append("(");
        if (dispatcherMethod.getParamNum() > 0){
            int n = 0;
            for (Type type : dispatcherMethod.getParameterTypes()){
                n++;
                if (n>1){
                    sb.append(", ");
                }
                sb.append(getRealName(type)).append(" ").append("arg").append(n);
            }
        }
        sb.append(");\n");
    }

    protected String getRealName(Type type){
        String name = type.getTypeName();
        if (name.startsWith("class ")){
            name = name.substring(6);
        }
        if (name.startsWith("java.lang.")){
            name = name.substring(10);
        }
        return name;
    }

    protected String getReturnName(Type type){
        return getRealName(type);
    }

}
