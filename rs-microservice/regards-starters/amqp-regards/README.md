# AMQP regards autoconfigure

## Regards namespace for rabbitmq

all Vhost defined and used by regards by regards ar prefixed by "Regards.amqp"

## How to run integration tests

To run integration test you have to have a rabbitmq-server running and copy `src/test/resources/application-rabbit.properties.sample` to `src/test/resources/application-rabbit.properties` and change any needed properties. If you don't have a running rabbitmq-server, the tests will be skipped.

--------------------------------------------------------------------------------

## How to use AMQP client

### Purpose of AMQP client

The only purpose of the AMQP client is to handle the multi tenancy communication with the message broker. That means creating virtual hosts on the broker dynamicly to ensure that each tenant's communications are isolated from the others'.

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

    public void publishExample() throws RabbitMQVhostException {
       Foo foo=new Foo("example");
       publisher_.publish(foo, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.INTERNAL);
    }
 }
```

This example send a message, representing the foo object, on the broker with the routing key : `fr.cnes.regards.example.Foo`.

NOTE: you can also publish message with different priority thanks to `publish(T object, AmqpCommunicationMode amqpCommunicationMode, AmqpCommunicationTarget amqpCommunicationTarget, int priority)`

### To subscribe to an object type

To subscribe to an object type, you can use the **Subcriber** class and it's **subscribeTo** method like so: `subscriber.subscribeTo(objectClass, pReceiver, pAmqpCommunicationMode, pAmqpCommunicationTarget)`.

#### Example

```java

  public void init() {
        receiverOneToMany = new TestReceiver();

        try {
            subscriberOneToManyExternal.subscribeTo(TestEvent.class, receiverOneToMany,
                                                    AmqpCommunicationMode.ONE_TO_MANY,
                                                    AmqpCommunicationTarget.EXTERNAL);

        } catch (RabbitMQVhostException e) {
            LOGGER.error(e.getMessage(), e);

        }
    }

    public void testSubscribeToOneToManyExternal() {
        final TestEvent toSend = new TestEvent("test one to many");
        final TenantWrapper<TestEvent> sended = new TenantWrapper<TestEvent>(toSend, TENANT);
        LOGGER.info(SENDED + sended);
        SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), TENANT);

        rabbitTemplate.convertAndSend(
                                      amqpConfiguration.getExchangeName(TestEvent.class.getName(),
                                                                        AmqpCommunicationTarget.EXTERNAL),
                                      amqpConfiguration.getRoutingKey("", AmqpCommunicationMode.ONE_TO_MANY), sended,
                                      pMessage -> {
                                          final MessageProperties propertiesWithPriority = pMessage
                                                  .getMessageProperties();
                                          propertiesWithPriority.setPriority(0);
                                          return new Message(pMessage.getBody(), propertiesWithPriority);
                                      });

        SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());

        try {
            Thread.sleep(SLEEP_TIME);
        } catch (InterruptedException e) {
            LOGGER.error(SLEEP_FAIL, e);

        }
        LOGGER.info(RECEIVED + receiverOneToMany.getMessage());

    }
```

### To poll an object

To poll an object from the message broker, you can use **Poller** class and it's **poll** method like so: `poller.poll(pTenant, objectClass, pAmqpCommunicationMode, pAmqpCommunicationTarget)`. That will give you a TenantWrapper containing the tenant, in whom behalf the message was sent, and the content.

#### Example

```java
  public void testPollOneToOneExternal() {
        final TenantWrapper<TestEvent> wrapperReceived;
        try {
            TestEvent sended= new TestEvent("message");
            publisher.publish(sended, AmqpCommunicationMode.ONE_TO_ONE, AmqpCommunicationTarget.EXTERNAL);
            //Assuming that TENANT represent the actual tenant
            wrapperReceived = poller.poll(TENANT, TestEvent.class, AmqpCommunicationMode.ONE_TO_ONE,
                                          AmqpCommunicationTarget.EXTERNAL);
            final TestEvent received = wrapperReceived.getContent();
        } catch (RabbitMQVhostException e) {
            final String msg = "Polling one to one Test Failed";
            LOGGER.error(msg, e);
            Assert.fail(msg);
        }
    }
```
