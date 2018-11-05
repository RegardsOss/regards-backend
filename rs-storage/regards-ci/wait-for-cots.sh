#!/bin/bash

/wait-for-it.sh -t 0 rs-rabbitmq:5672 --strict -- echo "rabbitmq is up"
if [ $? -ne 0 ]
then
    echo $*
    exit -1
fi

/wait-for-it.sh -t 0 rs-postgres:5432 --strict -- echo "postgres is up"
if [ $? -ne 0 ]
then
    echo $*
    exit -1
fi
