package fr.cnes.regards.modules.templates.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.templates.domain.Template;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class TestConfiguration {

    @Bean
    public Template testTemplate() {
        return new Template("name", "content");
    }

}
