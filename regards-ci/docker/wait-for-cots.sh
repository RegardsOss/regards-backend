#!/bin/bash -xe

IFS=' ' read -ra COTS <<<"${COTS}"
for COT in "${COTS[@]}"; do
    /wait-for-it.sh -t 0 ${COT}
done
