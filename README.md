# REGARDS OSS BACKEND

This repository contains all sources for the following components:

- REGARDS framework
- REGARDS microservices based on REGARDS framework

# Build

## Requirements

### The build relies on

* Maven v3.8.4+
* JDK Eclipse Temurin v17.0.3+

#### For docker images generations
* Docker engine v27+ (https://docs.docker.com/engine/install/rhel/)

### Prerequisite tools

* Elasticsearch 7.17.22
* PostgreSQL 11
* RabbitMQ 3.11

### Environment prerequisites

#### For compilation, generation and unit testing 
To compile, generate, and perform unit testing, a computer or virtual machine with the following specifications is required:
* CPU : 64-bit with at least 4 threads, clocked at 2.5 GHz or higher (e.g., Intel Core i5 8th generation or equivalent) 
* RAM : 12 GB or more
* Disk space : 50 GB available
* Operating System : Red Hat Enterprise Linux 8.x (64-bit)

#### For integration testing
For integration tests a computer or virtual machine with the following specifications is required:
* CPU : 64-bit with at least 4 threads, clocked at 2.5 GHz or higher (e.g., Intel Core i5 8th generation or equivalent) 
* RAM : 16 GB or more
* Disk space : 50 GB available
* Operating System : Red Hat Enterprise Linux 8.x (64-bit)

### Maven Configuration
#### Environment variables
The following environment variables are required
 - `REGARDS_HOME`: Used by the compilation process to locate the source files.
 - `MAVEN_HOME`: Defines the Maven home directory for configuration and dependencies repository.
 - `REGARDS_DOCKER_IMAGE_TAG`: Tag used to generate the REGARDS Docker image (default: latest).

#### settings.xml
The dependencies repository has to be configured in the .m2/settings.xml
```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <localRepository>${MAVEN_HOME}/repository</localRepository>
    <interactiveMode>true</interactiveMode>
    <offline>false</offline>
</settings>
``` 

#### Profiles
The Maven compilation profiles are:
 - `LT` : (LocalTest) Used to run maven integration tests with locally installed COTS. Access to COTS are configured in the LT.properties file
 - `RT` : (RemoteTest) Used to run maven integration tests with remote installed COTS. Access to COTS are configured in the RT.properties file
 - `CI` : (ContinuousIntegration) Used to run maven integration tests in Jenkins CI environement. Access to COTS are configured in the ${env.MAVEN_HOME}/conf/CI.properties file
 - `docker` : Used to generate microservice Docker images
 - `delivery` : Used to generate microservices executables JAR

#### Non central repository
Some dependencies won't be found in the Maven central repository. You need to ensure that the depenencies are present in the used repository or to add the official external repositories. 
 - [GeoTools](https://docs.geotools.org/latest/userguide/tutorial/quickstart/maven.html)
 
## How to

### Build the app locally 

```bash
cd <build_directory>
git clone https://github.com/RegardsOss/regards-backend.git
cd regards-backend
export REGARDS_HOME=<build_directory>/regards-backend
export MAVEN_HOME=<buil_directory>/maven
mvn clean install -DskipTests -P delivery
```

Compilation may take some time but can be shortened by using multithreaded compilation if your CPU supports it: 
```bash
mvn clean package -DskipTests delivery -T 4
```


#### Expected results
Expected jars for version X.Y.Z are:

- <build_directory>/regards-backend/rs-access/bootstrap-access-instance-light/target/bootstrap-access-instance-light-X.Y.Z.jar
- <build_directory>/regards-backend/rs-access/bootstrap-access-instance/target/bootstrap-access-instance-X.Y.Z.jar
- <build_directory>/regards-backend/rs-access/bootstrap-access-project/target/bootstrap-access-project-X.Y.Z.jar
- <build_directory>/regards-backend/rs-admin/bootstrap-administration-instance/target/bootstrap-administration-instance-X.Y.Z.jar
- <build_directory>/regards-backend/rs-admin/bootstrap-administration/target/bootstrap-administration-X.Y.Z.jar
- <build_directory>/regards-backend/rs-catalog/bootstrap-catalog/target/bootstrap-catalog-X.Y.Z.jar
- <build_directory>/regards-backend/rs-cloud/rs-authentication/bootstrap-authentication/target/bootstrap-authentication-X.Y.Z.jar
- <build_directory>/regards-backend/rs-cloud/rs-config/bootstrap-config/target/bootstrap-config-X.Y.Z.jar
- <build_directory>/regards-backend/rs-cloud/rs-gateway/bootstrap-gateway/target/bootstrap-gateway-X.Y.Z.jar
- <build_directory>/regards-backend/rs-cloud/rs-registry/bootstrap-registry/target/bootstrap-registry-X.Y.Z.jar
- <build_directory>/regards-backend/rs-dam/bootstrap-dam/target/bootstrap-dam-X.Y.Z.jar
- <build_directory>/regards-backend/rs-dataprovider/bootstrap-dataprovider/target/bootstrap-dataprovider-X.Y.Z.jar
- <build_directory>/regards-backend/rs-delivery/bootstrap-delivery/target/bootstrap-delivery-X.Y.Z.jar
- <build_directory>/regards-backend/rs-fem/bootstrap-fem/target/bootstrap-fem-X.Y.Z.jar
- <build_directory>/regards-backend/rs-file-access/bootstrap-file-access/target/bootstrap-file-access-X.Y.Z.jar
- <build_directory>/regards-backend/rs-file-catalog/bootstrap-file-catalog/target/bootstrap-file-catalog-X.Y.Z.jar
- <build_directory>/regards-backend/rs-file-packager/bootstrap-file-packager/target/bootstrap-file-packager-X.Y.Z.jar
- <build_directory>/regards-backend/rs-ingest/bootstrap-ingest/target/bootstrap-ingest-X.Y.Z.jar
- <build_directory>/regards-backend/rs-lta-manager/bootstrap-lta-manager/target/bootstrap-lta-manager-X.Y.Z.jar
- <build_directory>/regards-backend/rs-notifier/bootstrap-notifier/target/bootstrap-notifier-X.Y.Z.jar
- <build_directory>/regards-backend/rs-order/bootstrap-order/target/bootstrap-order-X.Y.Z.jar
- <build_directory>/regards-backend/rs-processing/bootstrap-processing/target/bootstrap-processing-X.Y.Z.jar
- <build_directory>/regards-backend/rs-storage/bootstrap-downloader/target/bootstrap-downloader-X.Y.Z.jar
- <build_directory>/regards-backend/rs-storage/bootstrap-storage/target/bootstrap-storage-X.Y.Z.jar
- <build_directory>/regards-backend/rs-worker-manager/bootstrap-worker-manager/target/bootstrap-worker-manager-X.Y.Z.jar

### Build the docker images

#### Microservice base image

All REGARDS microservices Docker images are based on the `regards-java-alpine` image.  
This image is accessible through the REGARDS github docker registry: `ghcr.io/regardsoss`.

If you want to use an alternate docker registry, you need to edit the root pom.xml to change the registry (the default being the regards official github package repository):
 - `docker.registry.host` : ghcr.io/regardsoss


#### Generation

To generate the docker images, REGARDS uses the [maven jib plugin](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin).

```bash
cd <build_directory>
git clone https://github.com/RegardsOss/regards-backend
export REGARDS_HOME=<build_directory>/regards-backend
export REGARDS_DOCKER_IMAGE_TAG=<desired tag>
cd regards-backend
mvn clean package jib:dockerBuild -P delivery,docker -B -Dfile.encoding=UTF-8 -Dmaven.test.skip -DimageTag=${REGARDS_DOCKER_IMAGE_TAG:=latest}
```

Compilation may take some time but can be shortened by using multithreaded compilation if your CPU supports it: 
```bash
mvn clean package jib:dockerBuild -P delivery,docker -B -Dfile.encoding=UTF-8 -Dmaven.test.skip -DimageTag=${REGARDS_DOCKER_IMAGE_TAG:=latest} -T 4
```

#### Expected results 

You can list locally generated docker images with the following  commands:
```bash
docker images --format "{{.Repository}}:{{.Tag}}" | egrep ".*/rs-.*:${REGARDS_DOCKER_IMAGE_TAG:=latest}$" | sort
```

Expected results with `tag` = `REGARDS_DOCKER_IMAGE_TAG` or `latest` if no one is specified: 
- <docker.registry.host>/rs-access-instance:<tag>
- <docker.registry.host>/rs-access-instance-light:<tag>
- <docker.registry.host>/rs-access-project:<tag>
- <docker.registry.host>/rs-admin-instance:<tag>
- <docker.registry.host>/rs-admin:<tag>
- <docker.registry.host>/rs-authentication:<tag>
- <docker.registry.host>/rs-catalog:<tag>
- <docker.registry.host>/rs-config:<tag>
- <docker.registry.host>/rs-dam:<tag>
- <docker.registry.host>/rs-dataprovider:<tag>
- <docker.registry.host>/rs-delivery:<tag>
- <docker.registry.host>/rs-downloader:<tag>
- <docker.registry.host>/rs-fem:<tag>
- <docker.registry.host>/rs-file-access:<tag>
- <docker.registry.host>/rs-file-catalog:<tag>
- <docker.registry.host>/rs-file-packager:<tag>
- <docker.registry.host>/rs-gateway:<tag>
- <docker.registry.host>/rs-ingest:<tag>
- <docker.registry.host>/rs-lta-manager:<tag>
- <docker.registry.host>/rs-notifier:<tag>
- <docker.registry.host>/rs-order:<tag>
- <docker.registry.host>/rs-processing:<tag>
- <docker.registry.host>/rs-registry:<tag>
- <docker.registry.host>/rs-storage:<tag>
- <docker.registry.host>/rs-worker-manager:<tag>


#### Push image to your docker repository

```bash
cd <build_directory>/regards-backend
docker tag <docker.registry.host>:${REGARDS_DOCKER_IMAGE_TAG:=latest} <your own docker registry host>:${REGARDS_DOCKER_IMAGE_TAG:=latest}
docker push <your own docker registry host>:${REGARDS_DOCKER_IMAGE_TAG:=latest}
```

## Tests environment requirements

### Unit test

There are no prerequisites to run REGARDS unit tests. Once compiled you can run the tests with the command: 
```bash
cd <build_directory>/regards-backend
mvn test
```

### Integration tests

The 4 following COTS are required to run REGARDS Integration tests:
 - Postgres
 - Elasticsearch
 - Rabbitmq
 - MinIO

Depending on the tests you want to run, all cots may not be required.
Access to these 4 COTS are configured in LT.properties, RT.properties or CI.properties files depending on which profile is used (LT, RT or CI).

This file should contain: 

```properties
# Variables for REGARDS Integration test

# Postgres
regards.IT.postgres.host=<Postgres server host address>
regards.IT.postgres.port=<Postgres server host port>
regards.IT.postgres.database=rs_testdb_tux
regards.IT.postgres.username=<Postgres server db user login>
regards.IT.postgres.password=<Postgres server db user password>

# ElasticSearch
regards.IT.elasticsearch.host=<Elasticsearch server host address>
regards.IT.elasticsearch.port=<Elasticsearch server host port>

# RabbitMQ
regards.IT.rabbitmq.host=<RabbitMQ server host address>
regards.IT.rabbitmq.port=<RabbitMQ server host port>
regards.IT.rabbitmq.management.host=<RabbitMQ server management (http access) address>
regards.IT.rabbitmq.management.port=<RabbitMQ server management (http access) port>

# MinIO
regards.IT.minio.host=<MinIO server host>
regards.IT.minio.port=<MinIO server port>
regards.IT.minio.protocol=http
```

**Note :** The `regards.IT.postgres.database` variable is set `to rs_testdb_tux` for local tests as the local docker container initializes this database at startup in `regards-ci/docker/postgres.init.ql`. This variable should be changed if you're using shared remote COTS as defined in the RT.properties file.

To use the LT profile and deploy those 4 COTS on a local environement you can use the local docker compose file with the command: 
```bash
cd <build_directory>
docker compose -f regards-ci/docker-compose-cots-local.yml up -d
```
Expected results: 
```bash
 ✔ Network regards_IT_network           Created
 ✔ Container docker-rs-elasticsearch-1  Started
 ✔ Container docker-rs-postgres-1       Started
 ✔ Container docker-rs-rabbitmq-1       Started
 ✔ Container docker-rs-minio-1          Started
```

Then you can run maven integration test with the here under command:
```bash
mvn -P LT integration-test
```

To stop the running COTS you can use the following command: 

```bash
cd <build_directory>
docker compose -f regards-ci/docker-compose-cots-local.yml down
```
Expected results: 
```bash
 ✔ Container docker-rs-rabbitmq-1       Removed
 ✔ Container docker-rs-postgres-1       Removed
 ✔ Container docker-rs-minio-1          Removed
 ✔ Container docker-rs-elasticsearch-1  Removed
 ✔ Network regards_IT_network           Removed
```


# Sources

*Classified by alphabetical order*

- regards-ci: This module contains all configuration files and scripts for Jenkins CI/CD.
- rs-access: This module contains all sources for access-project and access-instance microservices. Those microservices handle the UI access to projects and the project management instance.
 [Check out the doc of access-project](https://regardsoss.github.io/docs/development/services/access-project/overview) and [access-instance](https://regardsoss.github.io/docs/development/services/access-instance/overview).
- rs-admin: This module contains all sources for administration and instance administration microservices. Those microservices handle mainly users and access rights.
 [Check out the doc!](https://regardsoss.github.io/docs/development/services/admin/overview).
- rs-bom: This module contains Maven build of materials.
- rs-catalog: The Catalog microservice handles the search engine to consult REGARDS catalog.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/catalog/overview).
- rs-cloud: This module contains REGARDS microservices that handle cloud communication between microservices (Config, Authentication, and Registry).
 [Check out the doc of config](https://regardsoss.github.io/docs/development/backend/services/config/overview), [authentication](https://regardsoss.github.io/docs/development/backend/services/authentication/overview), and [Registry](https://regardsoss.github.io/docs/development/backend/services/registry/overview).
- rs-dam: The Data Management microservice handles REGARDS catalog construction with data models and data crawlers.
 [Check out the doc!](https://regardsoss.github.io/docs/development/services/dam/overview).
- rs-dataprovider: The Data provider microservice generates products from scanned files on the file system.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/dataprovider/overview).
- rs-delivery: The Delivery microservice allows users to order products and retrieve these ordered files on a provided storage location.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/delivery/overview).
- rs-file-access: The File Access microservice handle the physical storage and access of files. 
[Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/file-access/overview)
- rs-file-catalog: The File Catalog microservice catalog all the stored and referenced files. 
[Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/file-catalog/overview)
- rs-file-packager: The File Packager microservice package files into archives before they are stored. 
[Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/file-packager/overview)
- rs-fem: The Feature manager microservice allows generating products from standard GeoJson features.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/fem/overview).
- rs-ingest: The Ingest microservice generates products with OAIS recommendation.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/ingest/overview).
- rs-lta-manager: The LTA Manager microservice is an interface to generate products for long-term archival.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/lta-manager/overview).
- rs-microservice: This module contains all sources for REGARDS framework used by the different microservices.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/framework/getting-started).
- rs-notifier: The Notifier microservice is responsible for broadcasting messages to configured recipients.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/notifier/overview).
- rs-order: The Order microservice prepares orders of files and allows users to download them.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/order/overview).
- rs-processing: The Processing microservice applies treatments to ordered files before they are served to the user.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/processing/overview).
- rs-storage: The Storage microservice handles storage and access to all files associated with a catalog of products.
 [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/storage/overview).
- rs-vendors: This module contains external code needed by the different microservices.
- rs-worker-manager: The Worker Manager is an interface between the REGARDS microservices and the REGARDS workers.
 [Check out the doc!](https://regardsoss.github.io/docs/development/services/worker-manager/overview).
