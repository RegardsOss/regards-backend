/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.autoconfigure;

import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.cnes.regards.framework.gson.strategy.GSonIgnoreExclusionStrategy;

/**
 * Customize message converter when GSON starter is on the class path.
 *
 * @author Marc Sordi
 *
 */
public class GsonWebConfiguration extends WebMvcConfigurerAdapter {

    /**
     * {@link GsonBuilder} bean
     */
    private final GsonBuilder builder;

    public GsonWebConfiguration(GsonBuilder pBuilder) {
        this.builder = pBuilder;
    }

    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> pConverters) {

        final Gson gson = builder.setExclusionStrategies(new GSonIgnoreExclusionStrategy()).create();

        final GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
        gsonHttpMessageConverter.setGson(gson);

        pConverters.add(gsonHttpMessageConverter);
        super.configureMessageConverters(pConverters);
    }
}
