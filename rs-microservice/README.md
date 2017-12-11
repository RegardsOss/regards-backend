# REGARDS microservice framework

This repository brings together all the REGARDS framework features :
* Archetypes for module and microservice generation,
* Core framework modules : plugin engine, job manager, etc.
* All framework starters,
* Test tools.

For now, this version has not yet been tested under operational conditions and is considered as **beta**.

## Build requirement

Build relies on :
* Maven 3+
* OpenJDK 8

Dependency : 
* REGARDS Bill Of Materials

## Build

```shell
mvn clean install -DskipTests
```