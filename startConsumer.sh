#!/bin/sh

CLASSPATH=.

for a in target/lib/*.jar ; do CLASSPATH=$CLASSPATH:$a; done;

echo "CLASSPATH:$CLASSPATH"

java -cp $CLASSPATH:target/classes:target/test-classes -Dname=MrConsumer -Djava.library.path=./jni cn.v5.rpc.demo.Consumer > consumer.log &
