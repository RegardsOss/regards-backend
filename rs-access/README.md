# Compilation de tous les projets

`mvn clean package`

# Compilation backend

`cd backend/backend-mock/ && mvn clean package`

# Start backend

`java -jar backend/backend-mock/target/backend-mock-1.0-SNAPSHOT.jar`

# Start frontend

``` bash
cd frontend/frontend-webapp/src/main/webapp
npm install
npm start
```

or

`java -jar frontend/frontend-boot/target/frontend-boot-0.0.1-SNAPSHOT.war`
