package fr.cnes.regards.modules.processing.testutils;

import fr.cnes.regards.framework.microservice.annotation.MicroserviceInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.contrib" })
@MicroserviceInfo(name = "processing-test", version = "2.0.0-SNAPSHOT")
public class TestReactiveApplication {

    public static void main(final String[] args) {
        SpringApplication app = new SpringApplication(TestReactiveApplication.class);
        app.setWebApplicationType(WebApplicationType.REACTIVE);
        app.run(args);
    }

}
