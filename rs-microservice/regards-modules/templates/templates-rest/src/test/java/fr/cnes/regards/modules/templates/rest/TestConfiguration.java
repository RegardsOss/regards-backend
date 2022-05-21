package fr.cnes.regards.modules.templates.rest;

import fr.cnes.regards.modules.templates.domain.Template;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
