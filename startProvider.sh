#!/bin/sh

CLASSPATH=.

for a in target/lib/*.jar ; do CLASSPATH=$CLASSPATH:$a; done;

echo "CLASSPATH:$CLASSPATH"

java -cp $CLASSPATH:target/classes:target/test-classes -Dname=MrProvider -Djava.library.path=./jni cn.v5.rpc.demo.Provider provider_cn_3 > provider.log &
