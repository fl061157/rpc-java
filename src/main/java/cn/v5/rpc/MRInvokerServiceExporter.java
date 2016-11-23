package cn.v5.rpc;


import cn.v5.mr.MRMessageListener;
import cn.v5.mr.MRSubscriber;
import org.springframework.remoting.rmi.RemoteInvocationSerializingExporter;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.io.*;

public class MRInvokerServiceExporter extends RemoteInvocationSerializingExporter implements MRMessageListener {

    private String serviceUrl;

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
    }

    protected byte[] process(byte[] bytes) throws IOException, ClassNotFoundException {
        RemoteInvocation invocation = readRemoteInvocation(bytes);
        RemoteInvocationResult result = invokeAndCreateResult(invocation, getProxy());
        return resultToByteArray(result);
    }

    protected RemoteInvocation readRemoteInvocation(byte[] bytes)
            throws IOException, ClassNotFoundException {

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = createObjectInputStream(bais);
        try {
            return doReadRemoteInvocation(ois);
        }
        finally {
            ois.close();
        }
    }

    protected byte[] resultToByteArray(RemoteInvocationResult result) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = createObjectOutputStream(baos);
        try {
            doWriteRemoteInvocationResult(result, oos);
        }
        finally {
            oos.close();
        }
        return baos.toByteArray();
    }

    @Override
    public void onMessage(MRSubscriber subscriber, long messageId, byte[] data) {
        try {
            byte[] out = process(data);
            subscriber.ackOk(messageId, out);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
