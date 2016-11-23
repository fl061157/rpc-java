package cn.v5.rpc;

public class NotifyRun {

    public static void run(Runnable runnable) {
        NotifyRunContext.set();
        try {
            runnable.run();
        } finally {
            NotifyRunContext.clean();
        }
    }

    public static void delayAndAlive(int delaySec, int aliveSec, Runnable runnable) {
        NotifyRunContext nrc = NotifyRunContext.has();
        NotifyRunContext.delayAndAlive(delaySec, aliveSec);
        try {
            runnable.run();
        } finally {
            NotifyRunContext.clean();
            if (nrc != null) {
                NotifyRunContext.set(nrc);
            }
        }
    }

    public static void delay(int delaySec, Runnable runnable) {
        delayAndAlive(delaySec, 0, runnable);
    }

    public static void alive(int aliveSec, Runnable runnable) {
        delayAndAlive(0, aliveSec, runnable);
    }
}

