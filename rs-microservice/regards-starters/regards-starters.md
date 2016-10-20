# REGARDS starter configuration

## Cloud starter

```properties
regards.cloud.name=false # Disable cloud support
```
## JPA instance starter

```properties
regards.jpa.instance.enabled=false # Disable JPA instance support
regards.jpa.instance.embedded=true # Activate embedded mode with HSQLDB
regards.jpa.instance.embeddedPath=target # Path for embedded databases files
regards.jpa.instance.dialect=org.hibernate.dialect.PostgreSQLDialect # Not mandatory if embedded mode is activated
regards.jpa.instance.datasource.url=jdbc:postgresql://localhost:5432/postgres # Not mandatory if embedded mode is activated
regards.jpa.instance.datasource.username=postgres # Not mandatory if embedded mode is activated
regards.jpa.instance.datasource.password=postgres # Not mandatory if embedded mode is activated
regards.jpa.instance.datasource.driverClassName=org.postgresql.Driver # Not mandatory if embedded mode is activated
```
## JPA multitenant starter

```properties
regards.jpa.multitenant.enabled=false # Disable JPA multitenant support
regards.jpa.multitenant.embedded=true # Activate embedded mode with HSQLDB
regards.jpa.multitenant.embeddedPath=target # Path for embedded databases files
regards.jpa.multitenant.dialect=org.hibernate.dialect.PostgreSQLDialect # Not mandatory if embedded mode is activated
regards.jpa.multitenant.tenants[<x>].name=example
regards.jpa.multitenant.tenants[<x>].datasource.url=jdbc:postgresql://localhost:5432/test1 # Not mandatory if embedded mode is activated
regards.jpa.multitenant.tenants[<x>].datasource.username=postgres # Not mandatory if embedded mode is activated
regards.jpa.multitenant.tenants[<x>].datasource.password=postgres # Not mandatory if embedded mode is activated 
regards.jpa.multitenant.tenants[<x>].datasource.driverClassName=org.postgresql.Driver # Not mandatory if embedded mode is activated

<x> : Integer value for tenant index starting with 0.
```

## Multitenant starter

### Default configuration

```properties
regards.tenants=PROJECT1, PROJECT2 # List of available tenants / Only useful for default configuration
```
### Customize tenant resolver

In a @Configuration file, define your bean as follow :

```java
@Bean
public ITenantResolver customTenantResolver() {
            return new CustomTenantResolver();
}
...
class CustomTenantResolver implements ITenantResolver {
...
}
```

## Security starter

```properties
jwt.secret=MTIzNDU2Nzg5 # Base64 encoded secret
```

## Swagger starter

Pre-requisite:
```properties
server.address=localhost # Spring boot server address
server.port=8080 # Spring boot server port
```
```properties
regards.swagger.enabled=false # Disable swagger support
regards.swagger.api-name=API name # API name
regards.swagger.api-title=API title # API title
regards.swagger.api-description=API description # API description
regards.swagger.api-license=API license # API license
regards.swagger.api-version=API version # API version
```

## Web socket starter

```properties
regards.websocket.enabled=false # Disable web socket support
```

