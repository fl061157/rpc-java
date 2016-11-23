package cn.v5.rpc.message;

public interface Messages {
    int REQUEST = 0;
    int RESPONSE = 1;
    int NOTIFY = 2;
    int REQUEST_WITH_TRACEID = 10;
    int NOTIFY_WITH_TRACEID = 12;

    String TRACE_ID = "TraceID";
}
