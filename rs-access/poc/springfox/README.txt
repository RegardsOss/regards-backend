
# Compilation
##############

mvn clean package

# Start server
##############

mvn spring-boot:run

# Test swagger
##############
From web browser 
Get api list ===>             http://localhost:8080/swagger-resources
Get api swagger json file =>  http://localhost:8080/v2/api-docs
Access Swagger UI ==>         http://localhost:8080/swagger-ui.html