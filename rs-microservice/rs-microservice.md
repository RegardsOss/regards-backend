# REGARDS Microservices

## Development context

REGARDS Microservices are REST services exposed by a Jetty web server contained in a Spring boot application and composed of modules. Each microservice is a Maven project aggregating Maven modules. There is two modules by default: one responsible for running the microservice and one responsible for business. In case a microservice needs to be composed by more than one business module, a module archetype is available.

Each module is responsible for its implementation(Java 8 or higher, Spring or not, etc)

Requirements for development:

- git client 1.8
- maven 3.x
- JDK 1.8

## Create a new microservice

To create a new microservice you have to create a new maven project with the microservice-archetype. To do so :

1. Clone the git rs-microservice repository<br>

  ```bash
  git clone https://user@thor.si.c-s.fr/git/rs-microservice
  ```

2. Compile and install the maven project<br>

  ```bash
  cd rs-microservice
  mvn clean install
  ```

3. Generate the new microservice maven project thanks to one of the following method:

  ```bash
  mvn archetype:generate
  ```

  choosing the right archetype(fr.cnes.regards.microservices:microservice-archetype) and answering the question or

  ```bash
  mvn archetype:generate
  -DarchetypeGroupId=fr.cnes.regards.microservices
  -DarchetypeArtifactId=microservices-archetype
  -DarchetypeVersion=0.0.1
  -DgroupId=my.microservice
  -DartifactId=myMicroService
  -DmoduleName=myModule
  -DarchetypeRepository=/path/to/git/repo/
  rs-microservice/microservice-archetype/target
  ```

By default the microservice archetype expose an exemple Rest Controller on <http://localhost:3333>

To change the microservice configuration modify the `myMicroService/bootstrap-myMicroService/src/main/resources/application.yml` file.

**To run the new microservice :**

```bash
cd myMicroService/bootstrap-myMicroservice
mvn spring-boot:run
```

**To authenticate :**

```bash
curl -X "POST" acme:acmesecret@localhost:3333/oauth/token
 -d grant_type=password
 -d username=[admin|user] -d password=[admin|user]
```

**API exemple access :**<br>

```bash
curl http://localhost:3333/api/greeting/ -H "Authorization: Bearer <user_acces_token>"
curl http://localhost:3333/api/me/ -H "Authorization: Bearer <admin_acces_token>"
```

**Swagger UI access :** <http://localhost:3333/swagger-ui.html>

**NOTE** : To add new Rest resource follow exemple on file `myModule/myModule-rest/src/main/java/fr/cnes/regards/modules/myModule/GreetingsController.java`

## Add a module

To add a new module to your microservice you have to add a new maven module with the module-archetype. To do so :

1. Go to myMicroService folder and run

  ```bash
  mvn archetype:generate
  ```

  choose the right archetype(fr.cnes.regards.modules:module-archetype) and precise information required or

  ```bash
  mvn archetype:generate
  -DarchetypeGroupId=fr.cnes.regards.modules
  -DarchetypeArtifactId=module-archetype
  -DarchetypeVersion=0.0.1
  -DgroupId=my.module
  -DartifactId=myNewModule
  -DarchetypeRepository=/path/to/git/repo/
  rs-microservice/module-archetype/target
  ```

2. Add the following dependency to `bootstrap-myMicroservice/pom.xml` file:

  ```xml
  <dependency>
  <groupId>fr.cnes.regards.modules.myNewModule</groupId>
  <artifactId>myNewModule-rest</artifactId>
  <version>1.0-SNAPSHOT</version>
  </dependency>
  ```

3. Modify the requestMapping in `myNewModule/myNewModule-rest/src/main/java/fr/cnes/regards/modules/myNewModule/rest/GreetingsController.java`

  ## Common features

Each microservice offer the features :

- OAuth2 authentication : <http://adress:port/oauth/token>
- REST Resources authorization access by user ROLES
- Access to the Cloud Eureka Regsitry client to communicate with others microservices
- Access to the Cloud Config Server to centralize configurations properties
- Allow CORS requests
- Swagger Interface : <http://adress:port/swagger-ui.html>
