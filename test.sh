#!/bin/sh

CLASSPATH=.

for a in target/dependency/*.jar ; do CLASSPATH=$CLASSPATH:$a; done;

echo "CLASSPATH:$CLASSPATH"

java -cp $CLASSPATH:target/classes:target/test-classes -Djava.library.path=../client/jni cn.v5.mr.test.MRClientTest
