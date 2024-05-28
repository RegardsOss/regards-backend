package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * This configuration class provides mvcHandlerMappingIntrospector bean because WebSecurityAutoConfiguration needs one through MvcRequestMatcher bean.
 * Usually this bean is provided by tomcat/jetty/... but here we are in a test configuration where no web server is present
 *
 * @author Olivier Rousselot
 */
@Configuration
public class TestConfiguration {

    @Bean(name = "mvcHandlerMappingIntrospector")
    public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
        return new HandlerMappingIntrospector();
    }
}
