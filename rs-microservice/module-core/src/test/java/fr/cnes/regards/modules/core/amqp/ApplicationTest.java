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

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(ApplicationTest.class, args);
    }

}
