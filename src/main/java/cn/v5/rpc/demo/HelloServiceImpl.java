package cn.v5.rpc.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class HelloServiceImpl implements HelloService {

    private static final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public String hello(String s) {

        if (logger.isDebugEnabled()) {
            logger.debug("Hello :{} ", s);
        }

        return "hello " + s;
    }

    @Override
    public boolean addUser(User user) {
        System.out.println("add user : " + user);
        return true;
    }

    @Override
    public User getUser(String name) {
        User user = new User(name, "email", 10, 1, 1.1);
        Address address = new Address("china", "sh", 2121);
        user.setAddress(address);
        return user;
    }

    @Override
    public int add(int v1, int v2) {
        return v1 + v2;
    }

    @Override
    public void callVoid() {
        System.out.println("xxxxxxxx callVoid xxxxxx");
    }

    @Override
    public void callVoid2(String v) {
        System.out.println("xxxxxxxx callVoid2 :" + v);
    }

    @Override
    public void exception() throws Exception {
        throw new Exception("hello service exception.");
    }

    @Override
    public int overloading(int a) {
        return a;
    }

    @Override
    public int overloading(String a) {
        return a == null ? 0 : a.length();
    }

    @Override
    public int overloading(String a, int b) {
        return a == null ? b : a.length() + b;
    }

    @Override
    public boolean callBoolean(boolean b) {
        System.out.println("callBoolean : " + b);
        return b;
    }

    @Override
    public byte callByte(byte b) {
        System.out.println("callByte : " + b);
        return b;
    }

    @Override
    public int callInt(int i) {
        System.out.println("callInt : " + i);
        return i;
    }

    @Override
    public long callLong(long l) {
        System.out.println("callLong : " + l);
        return l;
    }

    @Override
    public float callFloat(float f) {
        System.out.println("callFloat : " + f);
        return f;
    }

    @Override
    public double callDouble(double d) {
        System.out.println("callDouble : " + d);
        return d;
    }

    @Override
    public String callString(String s) {
        System.out.println("callString : " + s);
        return s;
    }

    @Override
    public List<String> callList(List<String> l) {
        System.out.println("callList : " + l);
        return l;
    }

    @Override
    public Map<String, String> callMap(Map<String, String> m) {
        System.out.println("callMap : " + m);
        return m;
    }
}
