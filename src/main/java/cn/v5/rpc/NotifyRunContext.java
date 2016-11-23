package cn.v5.rpc;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;

public class NotifyRunContext implements Serializable {

    private static final ThreadLocal<NotifyRunContext> localeContextHolder =
            new ThreadLocal<>();

    private int delay;
    private int alive;

    public NotifyRunContext() {
    }

    public NotifyRunContext(int delay, int alive) {
        this.delay = delay;
        this.alive = alive;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getAlive() {
        return alive;
    }

    public void setAlive(int alive) {
        this.alive = alive;
    }

    public static NotifyRunContext has() {
        return localeContextHolder.get();
    }

    public static void set() {
        localeContextHolder.set(new NotifyRunContext());
    }

    public static void set(NotifyRunContext nrc) {
        localeContextHolder.set(nrc);
    }

    public static void delay(int delay) {
        localeContextHolder.set(new NotifyRunContext(delay, 0));
    }

    public static void alive(int alive) {
        localeContextHolder.set(new NotifyRunContext(0, alive));
    }

    public static void delayAndAlive(int delay, int alive) {
        localeContextHolder.set(new NotifyRunContext(delay, alive));
    }

    public static void clean() {
        localeContextHolder.remove();
    }

    public String toString(){
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
}
