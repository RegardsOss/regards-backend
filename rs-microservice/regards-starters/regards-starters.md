# REGARDS starter configuration

## Cloud starter

```properties
regards.cloud.name=false # Disable cloud support
```
## JPA instance starter

```properties
regards.jpa.instance.enabled=false # Disable JPA instance support
```
TODO : add datasource configuration

## JPA multitenant starter

```properties
regards.jpa.multitenant.enabled=false # Disable JPA multitenant support
```
TODO : add datasource configuration

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

