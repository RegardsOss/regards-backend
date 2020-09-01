package fr.cnes.regards.modules.processing.config;

import fr.cnes.regards.modules.processing.rest.PBatchController;
import fr.cnes.regards.modules.processing.rest.PMonitoringController;
import fr.cnes.regards.modules.processing.rest.PProcessController;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;

import javax.annotation.PostConstruct;

@Configuration
@EnableWebFlux
@ComponentScan(basePackageClasses = {
    PBatchController.class,
    PProcessController.class,
    PMonitoringController.class
})
public class ProcessingRestConfiguration {

    @PostConstruct
    public void init() {

    }

}
