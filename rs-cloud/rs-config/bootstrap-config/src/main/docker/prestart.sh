#!/usr/bin/env bash

jar -xvf /bootstrap-config-2.0.0-SNAPSHOT.jar BOOT-INF/classes/regards/application.properties
sed -i s/\$\{regards\.registry\.host}/"$REGISTRY_URL"/g BOOT-INF/classes/regards/application.properties
cat BOOT-INF/classes/regards/application.properties
jar -uvf /bootstrap-config-2.0.0-SNAPSHOT.jar BOOT-INF/classes/regards/application.properties
