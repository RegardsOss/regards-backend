/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.core.amqp;
/*
 * LICENSE_PLACEHOLDER
 */

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author svissier
 *
 */
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.security.utils", "fr.cnes.regards.modules.core" })
public class ApplicationTest {

    // @Autowired
    // private Subscriber subscriber_;
    //
    // @Bean
    // public TestReceiver Receiver() {
    // return new TestReceiver();
    // }
    //
    // @Bean
    // public SimpleMessageListenerContainer container(TestReceiver pReceiver, ConnectionFactory pConnectionFactory) {
    // return subscriber_.subscribeTo(TestEvent.class, pReceiver, "receive", pConnectionFactory);
    // }

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(ApplicationTest.class, args);
    }

}
