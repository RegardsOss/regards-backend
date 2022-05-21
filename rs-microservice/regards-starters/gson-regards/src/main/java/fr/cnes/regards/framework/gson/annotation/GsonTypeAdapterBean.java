package fr.cnes.regards.framework.gson.annotation;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identify a GSON {@link TypeAdapter} bean to be added dynamically to the {@link GsonBuilder} at startup
 *
 * @author Marc Sordi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface GsonTypeAdapterBean {

    Class<?> adapted();
}
