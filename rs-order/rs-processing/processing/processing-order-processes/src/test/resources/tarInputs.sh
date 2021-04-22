#!/usr/bin/env bash

pwd

ls -la

ls -la input/

sleep 5

tar -cf "output/${OUTPUT_NAME}.tar" input

ls -la output/