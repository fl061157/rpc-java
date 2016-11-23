package cn.v5.rpc.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HelloServiceImpl implements HelloService {
    private static Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public String hello(String s) {
        //logger.debug("hello {}",  s);
        return "hello " + s;
    }

    @Override
    public boolean addUser(User user) {
        logger.debug("add user : " + user);
        return true;
    }

    @Override
    public boolean addUsers(List<User> users) {
        if (users != null) {
            users.forEach(user -> logger.debug("add users : " + user));
        }
        return true;
    }

    @Override
    public User getUser(String name) {
        logger.debug("get user {}", name);
        User user = new User(name, "email", 10, 1, 1.1);
        Address address = new Address("china", "sh", 2121);
        user.setAddress(address);
        return user;
    }

    @Override
    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        users.add(new User("name0", "email", 10, 1, 1.1));
        users.add(new User("name1", "email", 10, 1, 1.1));
        users.add(new User("name2", "email", 10, 1, 1.1));
        users.add(new User("name3", "email", 10, 1, 1.1));
        users.add(new User("name4", "email", 10, 1, 1.1));

        return users;
    }

    @Override
    public Map<String, User> addMapUsers(Map<String, User> map) {
        if (map != null){
            map.forEach((name, user) -> logger.info("name:{}, user:{}", name, user));
        }
        return map;
    }

    @Override
    public int add(int v1, int v2) {
        logger.debug("add {} + {}", v1, v2);
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
        logger.debug("exception.");
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
        logger.debug("callBoolean : " + b);
        return b;
    }

    @Override
    public byte callByte(byte b) {
        logger.debug("callByte : " + b);
        return b;
    }

    @Override
    public int callInt(int i) {
        logger.debug("callInt : " + i);
        return i;
    }

    @Override
    public long callLong(long l) {
        logger.debug("callLong : " + l);
        return l;
    }

    @Override
    public float callFloat(float f) {
        logger.debug("callFloat : " + f);
        return f;
    }

    @Override
    public double callDouble(double d) {
        logger.debug("callDouble : " + d);
        return d;
    }

    @Override
    public String callString(String s) {
        logger.debug("callString : " + s);
        return s;
    }

    @Override
    public List<String> callList(List<String> l) {
        logger.debug("callList : " + l);
        return l;
    }

    @Override
    public Map<String, String> callMap(Map<String, String> m) {
        logger.debug("callMap : " + m);
        return m;
    }

    @Override
    public byte[] getBytes(byte[] data) {
        return data;
    }

    @Override
    public int[] getInts(int[] data) {
        return data;
    }

    @Override
    public User[] getUsers2(User[] data) {
        return data;
    }
}
