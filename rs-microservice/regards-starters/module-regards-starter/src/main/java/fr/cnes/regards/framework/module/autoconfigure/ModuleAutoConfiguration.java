/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.module.autoconfigure;

import javax.validation.Validator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 *
 * Module auto configuration
 *
 * @author Marc Sordi
 *
 */
@Configuration
@ConditionalOnWebApplication
public class ModuleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Validator localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }
}
