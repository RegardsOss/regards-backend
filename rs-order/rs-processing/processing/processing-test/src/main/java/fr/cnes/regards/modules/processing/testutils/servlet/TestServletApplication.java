package fr.cnes.regards.modules.processing.testutils.servlet;

import fr.cnes.regards.framework.microservice.annotation.MicroserviceInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.contrib" })
@MicroserviceInfo(name = "processing-test", version = "2.0.0-SNAPSHOT")
public class TestServletApplication {

    public static void main(final String[] args) {
        SpringApplication app = new SpringApplication(TestServletApplication.class);
        app.setWebApplicationType(WebApplicationType.SERVLET);
        app.run(args);
    }

}
