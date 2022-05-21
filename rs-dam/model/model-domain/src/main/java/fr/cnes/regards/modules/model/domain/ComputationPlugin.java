package fr.cnes.regards.modules.model.domain;

import fr.cnes.regards.modules.model.dto.properties.PropertyType;

import java.lang.annotation.*;

/**
 * Specific annotation to add to plugin
 * for {@link IComputedAttribute} to specify which type the implementation can support.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ComputationPlugin {

    /**
     * @return {@link PropertyType} supported by the plugin annotation
     */
    PropertyType supportedType();

}
