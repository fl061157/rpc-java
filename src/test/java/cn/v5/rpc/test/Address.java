package cn.v5.rpc.test;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.msgpack.annotation.Index;
import org.msgpack.annotation.Message;

import java.io.Serializable;

@Message
public class Address implements Serializable {

    @Index(0)
    private String country;
    @Index(1)
    private String city;
    @Index(2)
    private int code;

    public Address() {
    }

    public Address(String country, String city, int code) {
        this.country = country;
        this.city = city;
        this.code = code;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }

}
