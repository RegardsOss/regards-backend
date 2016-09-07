# Add a module

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
