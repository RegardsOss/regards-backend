/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Feign auto configuration with profile restriction to ease mocking<br/>
 * If test profile is used, all feign clients in regards package have to be mocked.
 *
 * @author Marc Sordi
 *
 */
@Profile("!test")
@Configuration
@EnableFeignClients("fr.cnes.regards")
@AutoConfigureAfter(name = "fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration")
public class FeignAutoConfiguration {
}
