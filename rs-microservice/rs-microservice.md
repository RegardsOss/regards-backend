# REGARDS Microservices

## Development context

REGARDS Microservices are REST services exposed by a Jetty web server contained in a Spring boot application and composed of modules. Each microservice is a Maven project aggregating Maven modules. There is two modules by default: one responsible for running the microservice and one responsible for business. In case a microservice needs to be composed by more than one business module, a module archetype is available.

Each module is responsible for its implementation(Java 8 or higher, Spring or not, etc)

Requirements for development:

- git client 1.8
- maven 3.x
- JDK 1.8

## Create a new microservice

To create a new microservice you have to create a new maven project with the microservice-archetype. To do so :<br>

1. Clone the git rs-microservice repository<br>

  ```bash
  git clone <https://user@thor.si.c-s.fr/git/rs-microservice>
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

  choosing the right archetype(local -> fr.cnes.regards.microservices:microservice-archetype) and answering the question or

  ```bash
  mvn archetype:generate
  -DarchetypeGroupId=fr.cnes.regards.microservices
  -DarchetypeArtifactId=rs-microservices-archetype
  -DarchetypeVersion=0.0.1
  -DgroupId=my.microservice
  -DartifactId=myMicroService
  -DmoduleName=myModule
  -DarchetypeRepository=/path/to/git/repo/
  rs-microservice/archetype/target
  ```

By default the microservice archetype expose an exemple Rest Controller on <http://localhost:3333><br>
To change the microservice configuration modify the `src/main/resources/application.yml` file.

**To run the new microservice :**

```bash
cd myMicroService/bootstrap-myMicroservice
mvn spring-boot:run
```

**To authenticate :**

```bash
curl -X "POST" acme:acmesecret@localhost:3333/oauth/token
 -d grant_type=password
 -d username=public -d password=public
```

**API exemple access :**<br>

```bash
curl <http://localhost:3333/api/greeting/> -H "Authorization: Bearer token"
curl <http://localhost:3333/api/me/> -H "Authorization: Bearer token"
```

**Swagger UI access :** <http://localhost:3333/swagger-ui.html>

**NOTE** : To add new Rest resource follow exemple on file `myModule/src/main/java/fr/cnes/regards/modules/myModule/GreetingController.java`

## Common features

Each microservice offer the features :

- OAuth2 authentication : <http://adress:port/oauth/token>
- REST Resources authorization access by user ROLES
- Access to the Cloud Eureka Regsitry client to communicate with others microservices
- Access to the Cloud Config Server to centralize configurations properties
- Allow CORS requests
- Swagger Interface : <http://adress:port/sawwer-ui.html>
