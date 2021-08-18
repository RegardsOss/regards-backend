# REGARDS OSS BACKEND

This repository contains all sources for :
 - REGARDS framework
 - REGARDS microservices based on REGARDS framework

# Build

## Requirements

Build relies on :
* Maven 3.3+
* OpenJDK 8

Prerequisite tools :
* Elasticsearch 6.7
* PostgreSQL 9.6+
* RabbitMQ 3.6.5

## How to

```bash
git clone https://github.com/RegardsOss/regards-oss-backend.git
cd regard-oss-backend
mvn clean install -DskipTests -P delivery 
```

# Sources

## regards-ci

 Contains all configuration files and scripts for Jenkins CI/CD

## rs-microservice
 
Contains all sources for REGARDS framework.
For more details go to online documentation at https://regardsoss.github.io/docs/development/backend/framework/getting-started/

## rs-access

Contains all sources for access-project and access-instance microservices.
Those microservices are two `backend for frontend` microservices.
For more details go to online documentation at https://regardsoss.github.io/docs/development/backend/services/access/overview/

## rs-admin

Contains all sources for administration and instance administration microservices.
Those microservices handle mainly users, and access rights.
For more details go to online documentation at https://regardsoss.github.io/docs/development/backend/services/admin/overview/

## rs-bom

Contains maven build of materials.

## rs-catalog

Contains all sources for catalog microservice. This microservice handle search engine to consult REGARDS catalog.
For more details go to online documentation at https://regardsoss.github.io/docs/development/backend/services/catalog/overview/


## rs-cloud

Contains all sources for REGARDS microservices that handles cloud communication between microservices :
 - Registry
 - Config
 - Authentication

For more detail go to online documentation at https://regardsoss.github.io/docs/development/backend/architecture/overview/


## rs-dam

Contains all sources for data management microservice. This service handle REGARDS catalog construction with data models and
data crawlers.
For more details go to online documentation at https://regardsoss.github.io/docs/development/backend/services/dam/overview/

## rs-dataprovider

Contains all sources for data provider microservice. This service, is used to generate products from scanned files on file system.
For more details go to online documentation at https://regardsoss.github.io/docs/development/backend/services/dataprovider/overview/   

## rs-fem

Contains all sources for feature manager microservice. This service, allows to generate products from standard GeoJson features.
For more details go to online documentation at https://regardsoss.github.io/docs/development/backend/services/fem/overview/

## rs-ingest

Contains all sources for ingest microservice. This service allows, to generate products with OAIS recommendation.
For more details go to online documentation at

## rs-notifier

Contains all sources for notifier microservice.
For more details go to online documentation at

## rs-order

Contains all sources for order and processing microservices.
For more details go to online documentation at https://regardsoss.github.io/docs/development/backend/services/ingest/overview/ and https://regardsoss.github.io/docs/development/backend/services/processing/overview/ 

For more information about OAIS recommendation https://regardsoss.github.io/docs/development/appendices/oais/

## rs-storage

Contains all sources for storage microservice. This service handle all access to all files associated to catalog products.
For more details go to online documentation at https://regardsoss.github.io/docs/development/backend/services/storage/overview/


