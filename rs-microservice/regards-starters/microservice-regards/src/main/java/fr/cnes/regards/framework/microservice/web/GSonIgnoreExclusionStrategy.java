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
        typeToSkip = null;
    }

    GSonIgnoreExclusionStrategy(Class<?> pTypeToSkip) {
        this.typeToSkip = pTypeToSkip;
    }

    @Override
    public boolean shouldSkipClass(Class<?> pClazz) {
        return (pClazz == typeToSkip);
    }

    @Override
    public boolean shouldSkipField(FieldAttributes pFieldAttributes) {
        return pFieldAttributes.getAnnotation(GSonIgnore.class) != null;
    }
}