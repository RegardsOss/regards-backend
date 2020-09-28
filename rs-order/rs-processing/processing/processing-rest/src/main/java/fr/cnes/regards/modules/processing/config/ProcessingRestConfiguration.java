package fr.cnes.regards.modules.processing.config;

import fr.cnes.regards.modules.processing.rest.PBatchReactiveController;
import fr.cnes.regards.modules.processing.rest.PMonitoringReactiveController;
import fr.cnes.regards.modules.processing.rest.PProcessReactiveController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")
@EnableWebFlux
@ComponentScan(basePackageClasses = {
    PBatchReactiveController.class,
    PProcessReactiveController.class,
    PMonitoringReactiveController.class
})
public class ProcessingRestConfiguration {

    @PostConstruct
    public void init() {

    }

}
