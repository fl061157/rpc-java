#!/bin/sh

CLASSPATH=.

for a in target/dependency/*.jar ; do CLASSPATH=$CLASSPATH:$a; done;

#echo "CLASSPATH:$CLASSPATH"

java -cp $CLASSPATH:target/classes:target/test-classes -Djava.library.path=jni cn.v5.mr.command.MRC $@
