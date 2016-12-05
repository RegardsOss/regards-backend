/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.microservice.web;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import fr.cnes.regards.framework.gson.annotation.GSonIgnore;

/**
 *
 * Class GSonIgnoreExclusionStrategy
 * {@link https://github.com/google/gson/blob/master/UserGuide.md#TOC-User-Defined-Exclusion-Strategies}
 *
 * @author Christophe Mertz
 */
public class GSonIgnoreExclusionStrategy implements ExclusionStrategy {

    private final Class<?> typeToSkip;

    GSonIgnoreExclusionStrategy() {
        typeToSkip=null;
    }
    
    GSonIgnoreExclusionStrategy(Class<?> typeToSkip) {
        this.typeToSkip = typeToSkip;
    }

    public boolean shouldSkipClass(Class<?> clazz) {
        return (clazz == typeToSkip);
    }

    public boolean shouldSkipField(FieldAttributes f) {
        return f.getAnnotation(GSonIgnore.class) != null;
    }
}