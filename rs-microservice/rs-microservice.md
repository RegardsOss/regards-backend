REGARDS Microservices
=====================

Development context
-------------------

REGARDS Microservices are REST services exposed by a Jetty web server contained in a Spring boot application.<br/> Each microservice is a Java 1.8 Maven project.

Requirements for development:

-	git client 1.8
-	maven 3.x
-	JDK 1.8

Create a new microservice
-------------------------

To create a new microservice you have to create a new maven project with the microservice-archetype. To do so :<br/>

1.	Clone the git rs-microservice repository<br/><code>git clone https://user@thor.si.c-s.fr/git/rs-microservice</code>
2.	Compile and install the maven project<br/><code> cd rs-microservice<br> mvn clean install</code>
3.	Generate the new microservice maven project<br/><code> mvn archetype:generate <br/> -DarchetypeGroupId=fr.cnes.regards.microservices<br/> -DarchetypeArtifactId=rs-microservices-archetype<br/> -DarchetypeVersion=0.0.1<br/> -DgroupId=<span style="color:yellow">my.microservice</span><br/> -DartifactId=<span style="color:yellow">myMicroService</span><br/> -DarchetypeRepository=<span style="color:yellow">/path/to/git/repo/</span>rs-microservice/archetype/target</code>

By default the microservice archetype expose an exemple Rest Controller on http://localhost:3333<br/> To change the microservice configuration modify the <span style='color:yellow'>src/main/resources/application.yml</span> file.

**To run the new microservice :**<br/><code>cd myMicroService<br/>mvn spring-boot:run</code>

**To authenticate :**<br/><code> curl -X "POST" acme:acmesecret@localhost:3333/oauth/token -d grant_type=password -d username=public -d password=public</code>

**API exemple access :**<br/><code>curl http://localhost:3333/api/greeting/ -H "Authorization: Bearer token"<br/>curl http://localhost:3333/api/me/ -H "Authorization: Bearer token"<br/></code>

**Swagger UI access :** http://localhost:3333/swagger-ui.html

**NOTE** : To add new Rest resource follow exemple on file <span style='color:yellow'>src/main/java/GreetingController.java</span>

Common features
---------------

Each microservice offer the features :

-	OAuth2 authentication : http://adress:port/oauth/token
-	REST Resources authorization access by user ROLES
-	Access to the Cloud Eureka Regsitry client to communicate with others microservices
-	Access to the Cloud Config Server to centralize configurations properties
-	Allow CORS requests
-	Swagger Interface : http://adress:port/sawwer-ui.html
