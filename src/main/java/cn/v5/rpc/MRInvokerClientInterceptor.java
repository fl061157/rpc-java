package cn.v5.rpc;

import cn.v5.mr.MRPublisher;
import cn.v5.mr.MessageResultContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.rmi.CodebaseAwareObjectInputStream;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedAccessor;
import org.springframework.remoting.support.RemoteInvocationResult;

import java.io.*;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;

public class MRInvokerClientInterceptor extends RemoteInvocationBasedAccessor
        implements MethodInterceptor, HttpInvokerClientConfiguration {

    private MRPublisher publisher;

    private String codebaseUrl;

    public void setPublisher(MRPublisher publisher) {
        this.publisher = publisher;
    }

    public void setCodebaseUrl(String codebaseUrl) {
        this.codebaseUrl = codebaseUrl;
    }

    @Override
    public String getCodebaseUrl() {
        return this.codebaseUrl;
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
            return "MR invoker proxy for service URL [" + getServiceUrl() + "]";
        }

        RemoteInvocation invocation = createRemoteInvocation(methodInvocation);
        ByteArrayOutputStream baos = getByteArrayOutputStream(invocation);

        byte[] out = invokeWithByteArray(baos.toByteArray());

        ByteArrayInputStream bais = new ByteArrayInputStream(out);
        RemoteInvocationResult result = readRemoteInvocationResult(bais, codebaseUrl);
        try {
            return recreateRemoteInvocationResult(result);
        } catch (Throwable ex) {
            if (result.hasInvocationTargetException()) {
                throw ex;
            } else {
                throw new RemoteInvocationFailureException("Invocation of method [" + methodInvocation.getMethod() +
                        "] failed in MR invoker remote service at [" + getServiceUrl() + "]", ex);
            }
        }
    }

    private byte[] invokeWithByteArray(byte[] bytes) throws Exception {
        MessageResultContext mrc = publisher.syncPubDirect(getServiceUrl(), bytes );
        if (mrc.getResult() != 0 || mrc.getBytes() == null) {
            throw new ConnectIOException("mr pub err. mrc = " + mrc.toString());
        }
        return mrc.getBytes();
    }

    protected RemoteInvocationResult readRemoteInvocationResult(InputStream is, String codebaseUrl)
            throws IOException, ClassNotFoundException {

        ObjectInputStream ois = createObjectInputStream(is, codebaseUrl);
        try {
            return doReadRemoteInvocationResult(ois);
        } finally {
            ois.close();
        }
    }

    protected RemoteInvocationResult doReadRemoteInvocationResult(ObjectInputStream ois)
            throws IOException, ClassNotFoundException {

        Object obj = ois.readObject();
        if (!(obj instanceof RemoteInvocationResult)) {
            throw new RemoteException("Deserialized object needs to be assignable to type [" +
                    RemoteInvocationResult.class.getName() + "]: " + obj);
        }
        return (RemoteInvocationResult) obj;
    }

    protected ObjectInputStream createObjectInputStream(InputStream is, String codebaseUrl) throws IOException {
        return new CodebaseAwareObjectInputStream(is, getBeanClassLoader(), codebaseUrl);
    }

    protected ByteArrayOutputStream getByteArrayOutputStream(RemoteInvocation invocation) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        writeRemoteInvocation(invocation, baos);
        return baos;
    }

    protected void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(os);
        try {
            oos.writeObject(invocation);
        } finally {
            oos.close();
        }
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();
    }
}
