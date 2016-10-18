# REGARDS Module Core

## How to run integration tests

--------------------------------------------------------------------------------

### For AMQP client

To run integration test you have to have a rabbitmq-server running and precise it's host and port in `src/test/resources/application.yml`.

If you can run docker and docker-compose, you can use the docker-compose.yml file located in `src/test/resources` to launch a rabbitmq-server.

## How to use AMQP client

### To publish a message

To publish a message on the broker, you can use the **Publisher** class and it's **publish** method like so: `publisher.publish(foo);`. Doing so, you will send a message representing the foo object to the exchange corresponding to the tenant parameterized in SecureContext at the time of invocation and with routing key set as the name of the parameter class.

#### Example

```java
package fr.cnes.regards.example;

public class Foo {
  private String content;

  public Foo(String content) {
    this.content=content;
  }
}

package fr.cnes.regards.example;

import org.springframework.beans.factory.annotation.Autowired;
import fr.cnes.regards.modules.core.amqp.Publisher;

public class Example {

    @Autowired
    private Publisher publisher_;

    public void publishExample() {
      Foo foo=new Foo("example");
      publisher_.publish(foo);
    }
}
```

This example send a message, representing the foo object, on the broker with the routing key : `fr.cnes.regards.example.Foo`.

### To subscribe to an object type

To subscribe to an object type, you can use the **Subcriber** class and it's **subscribeTo** method like so: `subscriber.subscribeTo(objectType, receiver, HandlingMethodName,connectionFactory)` inside a Bean.

#### Example
