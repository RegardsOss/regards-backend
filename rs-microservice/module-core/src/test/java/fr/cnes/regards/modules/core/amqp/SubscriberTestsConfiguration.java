/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "fr.cnes.regards.security.utils", "fr.cnes.regards.modules.core" })
public class SubscriberTestsConfiguration {

    // @Bean
    // public SimpleMessageListenerContainer container() {
    // receiver_ = new TestReceiver();
    // return subscriber_.subscribeTo(TestEvent.class, receiver_, "receive", connectionFactory_);
    // }

}
