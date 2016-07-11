#!/bin/bash
files=$(find . -type f -name "*.ts")

for file in $files ; do

	name=$(basename $file ".ts")
	dir=$(dirname $file)
	mv $file $dir/$name.tsx
done
