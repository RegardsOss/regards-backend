import fr.cnes.regards.framework.microservice.annotation.MicroserviceInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Start microservice ${parentArtifactId}
 *
 * @author TODO
 */
@SpringBootApplication(scanBasePackages = { "fr.cnes.regards.modules", "fr.cnes.regards.contrib" })
@MicroserviceInfo(name = "${parentArtifactId}", version = "${version}")
public class Application {

    /**
     * Microservice bootstrap method
     *
     * @param pArgs microservice bootstrap arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
