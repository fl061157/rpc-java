package cn.v5.rpc.test;

import cn.v5.rpc.annotation.RpcIgnore;
import cn.v5.rpc.annotation.RpcMethod;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Map;

public interface HelloService {

    @Cacheable(value = "hello")
    String hello(String who);
    boolean addUser(User user);
    boolean addUsers(List<User> users);
    User getUser(String name);
    List<User> getUsers();

    Map<String, User> addMapUsers(Map<String, User> map);

    int add(int v1, int v2);
    void callVoid();
    void callVoid2(String v);
    void exception() throws Exception;

    int overloading(int a);

    @RpcMethod(alias = "overloading_string")
    int overloading(String a);

    @RpcMethod(alias = "overloading_string_int")
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

    byte[] getBytes(byte[] data);
    int[] getInts(int[] data);
    User[] getUsers2(User[] data);

}
