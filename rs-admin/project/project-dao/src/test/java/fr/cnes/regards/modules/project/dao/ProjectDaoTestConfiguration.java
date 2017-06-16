/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.dao;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 *
 * Class ProjectDaoTestConfiguration
 *
 * Configuration class for DAO tests.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:tests.properties")
public class ProjectDaoTestConfiguration {

    @Bean
    public MBeanExporter mBeanExporter() {
        MBeanExporter exporter=new MBeanExporter();
        exporter.setRegistrationPolicy(RegistrationPolicy.IGNORE_EXISTING);
        return exporter;
    }

}
