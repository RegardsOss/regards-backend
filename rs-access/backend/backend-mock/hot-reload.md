## How-to
Pour le moment, tant qu'il n'y a pas de Bearer de développement statique pour
s'authentifier en administrateur il n'est pas possible d'utiliser le package `spring-boot-devtools`.
Car il relance totalement l'application lors d'un changement


## Pour le moment

Télécharger le dernier jar de springloaded:
https://repo.spring.io/libs-snapshot-local/org/springframework/springloaded/1.2.7.BUILD-SNAPSHOT/springloaded-1.2.7.BUILD-20160411.213134-1.jar
Modifier votre lanceur du project backend-mock pour spécifier les paramètres JVM suivant:

```
# Déjà une bonne base est de limiter sa RAM
-Xms10M -Xmx75M -Xss1m
# Lorsqu'il y a un make du project, l'application charge les nouveautés (sans relancer toute l'application)
-javaagent:/home/lmieulet/CS/rs-access/backend/backend-mock/src/main/resources/springloaded-1.2.7.RELEASE.jar -noverify
```
