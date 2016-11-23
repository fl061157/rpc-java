package cn.v5.rpc.annotation;


import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
@Inherited
public @interface RpcIgnore {
}
