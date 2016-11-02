# REGARDS starter configuration

## AMQP starter
```properties
spring.rabbitmq.addresses= 172.26.47.52:5672 # spring property indicating the message broker addresses, the amqp starter only can handle one address
spring.rabbitmq.username=guest # spring property indicating the username used to connect and manage the broker, for the amqp starter, this user must have permissions to add virtual hosts and permissions
spring.rabbitmq.password=guest # password of the user

regards.amqp.microservice.typeIdentifier="TOTO" # property used to name exchanges and queues private to this type of microservices
regards.amqp.microservice.instanceIdentifier="TOTO_inst1" # property used to name exchanges and queues only related to an instance (mainly used in broadcast)

regards.amqp.management.host= 172.26.47.52 # address of the managing plugin of the broker
regards.amqp.management.port= 15672 # port of the managing plugin of the broker
```

## Cloud starter

```properties
regards.cloud.name=false # Disable cloud support
```

## HATEOAS starter

WARNING : at the moment, this starter only accepts **@RequestMapping** annotation to work fine.

If your controller manages a single POJO, implements **IResourceController<T>**.

In its **toResource** method, define all links using an autowired **IResourceService** regardless security authorization. Only authorized links will be really exposed.

Sample :
```java
@RestController
@RequestMapping("/pojos")
public class MyPojoController implements IResourceController<MyPojo> {
	...	
	@Autowired
	private IResourceService resourceService;
	...
    @ResourceAccess(description = "List all pojos")
    @GetMapping
    public ResponseEntity<List<Resource<MyPojo>>> getPojos() {
        final List<MyPojo> pojos = ...
        return ResponseEntity.ok(toResources(pojos));
    }
	...
    @ResourceAccess(description = "Get a pojo")
    @GetMapping("/{pPojoId}")
    public ResponseEntity<Resource<MyPojo>> getPojo(@PathVariable Long pPojoId) {
        final MyPojo pojo = ...
        return ResponseEntity.ok(toResource(pojo));
    }
	...
	@Override
    public Resource<MyPojo> toResource(MyPojo pPojo) {
        final Resource<MyPojo> resource = resourceService.toResource(pPojo);
        resourceService.addLink(resource, this.getClass(), "getPojo", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pPojo.getId()));
        ...
        return resource;
    }
}
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

### Use transaction with multitenant

You can annotated your service or test with the following meta annotation to manage transaction:
```java
@MultitenantTransactional
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
jwt.secret=MTIzNDU2Nzg5 # Secret key
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

