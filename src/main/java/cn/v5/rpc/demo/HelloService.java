package cn.v5.rpc.demo;

import cn.v5.rpc.annotation.RpcMethod;

import java.util.List;
import java.util.Map;

public interface HelloService {
    String hello(String s);
    boolean addUser(User user);
    User getUser(String name);
    int add(int v1, int v2);
    void callVoid();
    void callVoid2(String v);
    void exception() throws Exception;

    int overloading(int a);
    int overloading(String a);

    @RpcMethod(alias = "overloading2")
    int overloading(String a, int b);

    boolean callBoolean(boolean b);
    byte callByte(byte b);
    int callInt(int i);
    long callLong(long l);
    float callFloat(float f);
    double callDouble(double d);
    String callString(String s);
    List<String> callList(List<String> l);
    Map<String, String> callMap(Map<String, String> m);

}
