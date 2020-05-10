#!/bin/bash
cd ..

root=../../../../../../..
req=${root}/JavaAdvanced/lib/

javac --module-path ${req} \
    --add-modules junit \
    --add-modules org.junit.jupiter.api \
    --add-modules org.junit.platform.commons \
    -d compiled *.java
#javac Server.java Client.java Account.java Bank.java LocalPerson.java Person.java RemoteAccount.java RemoteBank.java RemotePerson.java AbstractPerson.java AbstractAccount.java LocalAccount.java
#rmic -d $CLASSPATH examples.rmi.RemoteAccount examples.rmi.RemoteBank
