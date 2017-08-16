#!/bin/bash -xe

/wait-for-it.sh -t 0 rs_rabbitmq:5672 --strict -- echo "rabbitmq is up"
/wait-for-it.sh -t 0 rs_postgres:5432 --strict -- echo "postgres is up"
