package cn.v5.rpc.dispatcher;


import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class DispatcherDescription {

    private String name;
    private Set<DispatcherMethod> dispatcherMethods = new HashSet<>();

    public DispatcherDescription(String name) {
        this.name = name;
    }

    public DispatcherDescription(String name, DispatcherMethod[] dms) {
        this(name);
        if (dms != null && dms.length > 0) {
            for (DispatcherMethod dm : dms) {
                dispatcherMethods.add(dm);
            }
        }
    }

    public String getName() {
        return name;
    }

    public Set<DispatcherMethod> getDispatcherMethods() {
        return dispatcherMethods;
    }

    public void addMethod(String name, Method method) {
        Type rt = method.getGenericReturnType();
        int pc = method.getParameterCount();
        Type[] pts = null;
        if (pc > 0) {
            pts = method.getGenericParameterTypes();
        }
        DispatcherMethod dm = new DispatcherMethod(name, rt, pts);
        dispatcherMethods.add(dm);
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

    public static class DispatcherMethod {
        private String name;
        private Type returnType;
        private Type[] parameterTypes;
        private int paramNum;

        public DispatcherMethod(String name, Type rtype, Type[] ps) {
            this.name = name;
            this.returnType = rtype;
            this.parameterTypes = ps;
            if (this.parameterTypes != null) {
                this.paramNum = this.parameterTypes.length;
            }
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Type getReturnType() {
            return returnType;
        }

        public void setReturnType(Type returnType) {
            this.returnType = returnType;
        }

        public Type[] getParameterTypes() {
            return parameterTypes;
        }

        public void setParameterTypes(Type[] parameterTypes) {
            this.parameterTypes = parameterTypes;
        }

        public int getParamNum() {
            return paramNum;
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
        }
    }
}
