package fr.cnes.regards.framework.modules.jobs.service;
/*
 * LICENSE_PLACEHOLDER
 */


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * @author svissier
 *
 */
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.framework.security.utils.jwt",
        "fr.cnes.regards.framework.amqp", "fr.cnes.regards.modules.project" })
public class Application {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * main
     *
     * @param pArgs
     *            args
     * @throws InterruptedException
     *             Exception
     */
    public static void main(String[] pArgs) throws InterruptedException {
        SpringApplication.run(Application.class, pArgs);
    }

}
