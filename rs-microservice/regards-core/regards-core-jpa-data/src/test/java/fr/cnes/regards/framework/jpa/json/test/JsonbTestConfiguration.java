/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.json.test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.json.GsonUtil;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "fr.cnes.regards.framework" })
@PropertySource("classpath:tests.properties")
public class JsonbTestConfiguration {

    @Bean
    public Void setGsonIntoGsonUtil(Gson pGson) {
        GsonUtil.setGson(pGson);
        return null;
    }

}
