# REGARDS OSS BACKEND

This repository contains all sources for :
 - REGARDS framework
 - REGARDS microservices based on REGARDS framework

# Build

## Requirements

Build relies on :
* Maven v3.8.4+
* JDK Eclipse Temurin v17.0.3+

Prerequisite tools :
* Elasticsearch 7.17.1
* PostgreSQL 9.6+
* RabbitMQ 3.8.2+

## How to

```bash
git clone https://github.com/RegardsOss/regards-backend
cd regards-backend
mvn clean install -DskipTests -P delivery 
```

# Sources

*Classified by alphabetical order*

- regards-ci: all configuration files and scripts for Jenkins CI/CD
- rs-microservice: all sources for REGARDS framework. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/framework/getting-started/)
- rs-access: all sources for access-project and access-instance microservices. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/access/overview/)
- rs-admin: all sources for administration and instance administration microservices. Those microservices handle mainly users, and access rights. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/admin/overview/)
- rs-bom: contains maven build of materials.
- rs-catalog: Catalog microservice handle search engine to consult REGARDS catalog. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/catalog/overview/)
- rs-cloud: REGARDS microservices that handles cloud communication between microservices (Registry, Config, Authentication). [Check out the doc!](https://regardsoss.github.io/docs/development/backend/architecture/overview/)
- rs-dam: Data Management microservice handles REGARDS catalog construction with data models and data crawlers. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/dam/overview/)
- rs-dataprovider: Data provider microservice generates products from scanned files on file system. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/dataprovider/overview/)
- rs-fem: Feature manager microservice allows to generate products from standard GeoJson features. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/fem/overview/)
- rs-ingest: Ingest microservice generates products with OAIS recommendation. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/ingest/overview/)
- rs-notifier: contains all sources for notifier microservice. [Check out Notifier documentation!](https://regardsoss.github.io/docs/development/backend/services/notifier/overview/)
- rs-order: contains all sources for order microservice. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/order/overview/)
- rs-processing: contains all sources for processing microservice. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/processing/overview/)
- rs-storage: Storage microservice handles all access to all files associated to catalog products. [Check out the doc!](https://regardsoss.github.io/docs/development/backend/services/storage/overview/)

