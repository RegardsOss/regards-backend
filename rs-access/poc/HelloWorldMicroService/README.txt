Exemple de microservice utilisant :
spring-security
spring-oauth2
springfox
spring-cloud-config : Connection au serveur ConfigServer (autre projet maven) sur le port 8888
spring-cloud-eureka : Connection au serveur EurekaServer (autre projet maven) sur le port 9999

1. Démarrer le serveur eureka :
cd ../EurekaServer
mvn spring-boot:run

1. Démarrer le serveur  de configuration :
cd ../ConfigServer
mvn spring-boot:run

3. Démarrer le microservice 
mvn spring-boot:run

4. Interogger depuis un navigateur web

http://localhost:7777/api/eureka/adress
Retourne les adresses des serveurs eureka et config

http://lolcahost:7777/api/cloud/config
Retoure le parametre my.otherproperty configurer sur le serveur de configuration