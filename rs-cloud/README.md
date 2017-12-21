# REGARDS cloud

This is the microservice infrastructure repository. It manages :
* System gateway (relying on Netflix Zuul),
* System authentication (relying on Spring OAuth2),
* Microservice registry for load balancing (relying on Netflix Eureka),
* Configuration (relying on Spring Cloud Config).

REGARDS is still under heavy development. Operational version V1.0.0 is planed for 2018.

## Build requirements

Build relies on :
* Maven 3+
* OpenJDK 8

Dependencies : 
* REGARDS Bill Of Materials
* REGARDS microservice framework
* REGARDS administration

## Build

```shell
mvn clean install
```
