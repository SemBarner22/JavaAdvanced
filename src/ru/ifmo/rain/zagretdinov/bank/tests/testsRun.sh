#!/bin/bash

cd ../../../../../../..

ROOT=$PWD

PACK_NAME=ru.ifmo.rain.zagretdinov.bank
#PACK_PATH=ru/ifmo/rain/zagretdinov/bank
TEST_CLASSES=("${PACK_NAME}.tests.AppTest"
              "${PACK_NAME}.tests.PersonTest"
              "${PACK_NAME}.tests.AccountTest")

#OUT_PATH=${ROOT}/out/production/${PACK_NAME}
#SOURCE_PATH=${ROOT}/java-solutions/${PACK_PATH}
LIB_PATH=${ROOT}/../java-advanced-2020/lib
JUNIT_JAR=${LIB_PATH}/junit-4.11.jar:${LIB_PATH}/hamcrest-core-1.3.jar

#rm -rf ${OUT_PATH}

#javac -cp .:${JUNIT_JAR} \
#      ${SOURCE_PATH}/*.java ${SOURCE_PATH}/tests/*.java ${SOURCE_PATH}/tests/base/*.java \
#      -d ${OUT_PATH}

#cd ${OUT_PATH} || exit 1

java -cp .:${JUNIT_JAR} org.junit.runner.JUnitCore "${TEST_CLASSES[@]}"

#exit ${?}