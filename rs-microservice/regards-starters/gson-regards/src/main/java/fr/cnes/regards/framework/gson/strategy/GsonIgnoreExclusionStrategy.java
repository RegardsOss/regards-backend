/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.gson.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;

/**
 *
 * Class GsonIgnoreExclusionStrategy
 * {@see <a>https://github.com/google/gson/blob/master/UserGuide.md#TOC-User-Defined-Exclusion-Strategies</a>}
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
public class GsonIgnoreExclusionStrategy implements ExclusionStrategy {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(GsonIgnoreExclusionStrategy.class);

    /**
     * Type to skip
     */
    private final Class<?> typeToSkip;

    public GsonIgnoreExclusionStrategy() {
        typeToSkip = null;
    }

    public GsonIgnoreExclusionStrategy(Class<?> pTypeToSkip) {
        this.typeToSkip = pTypeToSkip;
    }

    @Override
    public boolean shouldSkipClass(Class<?> pClazz) {
        return pClazz == typeToSkip;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes pFieldAttributes) {
        final boolean isSkipped = pFieldAttributes.getAnnotation(GsonIgnore.class) != null;
        if (isSkipped) {
            LOG.debug(String.format("Skipping field %s in class %s.", pFieldAttributes.getName(),
                                    pFieldAttributes.getClass()));
        }
        return isSkipped;
    }
}
