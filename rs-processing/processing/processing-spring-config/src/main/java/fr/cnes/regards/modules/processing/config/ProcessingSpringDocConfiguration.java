package fr.cnes.regards.modules.processing.config;

import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.SpringDocUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * This class is the configuration for SpringDoc.
 *
 * @author gandrieu
 */
@Configuration
public class ProcessingSpringDocConfiguration implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        // initSpringDoc
        SpringDocUtils.getConfig()
                      .replaceWithSchema(Duration.class, new NumberSchema().description("Duration in nanoseconds"))
                      .replaceWithSchema(OffsetDateTime.class,
                                         new StringSchema().description("ISO formatted UTC date-time")
                                                           .format("date-time")
                                                           .example("2020-12-31T00:00:00.000Z"))
                      .replaceWithClass(io.vavr.collection.Set.class, java.util.Set.class)
                      .replaceWithClass(io.vavr.collection.Seq.class, java.util.List.class)
                      .replaceWithClass(io.vavr.collection.List.class, java.util.List.class)
                      .replaceWithClass(io.vavr.collection.Map.class, java.util.Map.class);
    }

}
