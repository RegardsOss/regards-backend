/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.framework.gson.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

import fr.cnes.regards.framework.gson.annotation.GSonIgnore;

/**
 *
 * Class GSonIgnoreExclusionStrategy
 * {@see <a>https://github.com/google/gson/blob/master/UserGuide.md#TOC-User-Defined-Exclusion-Strategies</a>}
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 */
public class GSonIgnoreExclusionStrategy implements ExclusionStrategy {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(GSonIgnoreExclusionStrategy.class);

    private final Class<?> typeToSkip;

    public GSonIgnoreExclusionStrategy() {
        typeToSkip = null;
    }

    public GSonIgnoreExclusionStrategy(Class<?> pTypeToSkip) {
        this.typeToSkip = pTypeToSkip;
    }

    @Override
    public boolean shouldSkipClass(Class<?> pClazz) {
        return pClazz == typeToSkip;
    }

    @Override
    public boolean shouldSkipField(FieldAttributes pFieldAttributes) {
        LOG.debug(String.format("name:%s - annotation present:%s", pFieldAttributes.getName(),
                                pFieldAttributes.getAnnotation(GSonIgnore.class) != null));
        return pFieldAttributes.getAnnotation(GSonIgnore.class) != null;
    }
}