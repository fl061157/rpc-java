package cn.v5.rpc.test;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.msgpack.annotation.*;

import java.io.Serializable;

@Message
public class User implements Serializable {

    @Index(0)
    private String name;
    @Index(1)
    private String email;
    @Index(2)
    private int age;
    @Index(3)
    private int sex;
    @Index(4)
    private double salary;
    @Index(5)
    @Optional
    private Address address;
    @Index(6)
    @Optional
    private double tel;


    public User() {
    }

    public User(String name, String email, int age, int sex, double salary) {
        this.name = name;
        this.email = email;
        this.age = age;
        this.sex = sex;
        this.salary = salary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public double getTel() {
        return tel;
    }

    public void setTel(double tel) {
        this.tel = tel;
    }
}
