#!/usr/bin/env bash

docker run \
        --rm \
        --name pg-docker \
        -e POSTGRES_USER=user \
        -e POSTGRES_PASSWORD=secret \
        -d \
        -p 5433:5432 \
        -v $HOME/.regards/postgres:/var/lib/postgresql/data \
    postgres:9.5

docker run \
        --name rabbit-docker \
        --rm \
        -d \
        -p 15672:15672 \
        -p 5672:5672 \
    rabbitmq:3.6.5-management