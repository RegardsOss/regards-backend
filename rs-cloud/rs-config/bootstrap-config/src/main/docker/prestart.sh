#!/usr/bin/env bash

jar -xvf /@project.build.finalName@.@project.packaging@ BOOT-INF/classes/regards/application.properties
sed -i s/\$\{regards\.registry\.host}/"$REGISTRY_URL"/g BOOT-INF/classes/regards/application.properties
cat BOOT-INF/classes/regards/application.properties
jar -uvf /@project.build.finalName@.@project.packaging@ BOOT-INF/classes/regards/application.properties
