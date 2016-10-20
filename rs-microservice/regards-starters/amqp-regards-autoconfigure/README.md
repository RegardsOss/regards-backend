# AMQP regards autoconfigure

## How to run integration tests

--------------------------------------------------------------------------------

### For AMQP client

To run integration test you have to have a rabbitmq-server running and copy `src/test/resources/application-rabbit.properties.sample` to `src/test/resources/application-rabbit.properties` and change any needed properties.

## How to use AMQP client

### To publish a message

To publish a message on the broker, you can use the **Publisher** class and it's **publish** method like so: `publisher.publish(foo, AmqpCommunicationMode.ONE_TO_ONE);`. Doing so, you will send a message representing the foo object to the exchange corresponding to the tenant parameterized in SecureContext at the time of invocation and with routing key set as the name of the parameter class.

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
       publisher_.publish(foo, AmqpCommunicationMode.ONE_TO_ONE);
    }
 }
```

This example send a message, representing the foo object, on the broker with the routing key : `fr.cnes.regards.example.Foo`.

NOTE: you can also publish message with different priority thanks to `publish(Object object, AmqpCommunicationMode amqpCommunicationMode, int priority)`

### To subscribe to an object type

To subscribe to an object type, you can use the **Subcriber** class and it's **subscribeTo** method like so: `subscriber.subscribeTo(objectClass, receiver)`.
