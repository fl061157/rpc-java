package cn.v5.rpc.error;

public class GiveupException extends RuntimeException {
    public GiveupException() {
        super();
    }

    public GiveupException(String message) {
        super(message);
    }

    public GiveupException(String message, Throwable cause) {
        super(message, cause);
    }

    public GiveupException(Throwable cause) {
        super(cause);
    }
}
