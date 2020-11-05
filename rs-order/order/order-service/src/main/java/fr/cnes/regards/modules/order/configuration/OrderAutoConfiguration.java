package fr.cnes.regards.modules.order.configuration;

import fr.cnes.regards.framework.gson.autoconfigure.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureBefore(GsonAutoConfiguration.class)
public class OrderAutoConfiguration {

}
