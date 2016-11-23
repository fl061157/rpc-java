package cn.v5.rpc.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
@Inherited
public @interface RpcService {
    String topic() default "";
    String face() default "";
}

