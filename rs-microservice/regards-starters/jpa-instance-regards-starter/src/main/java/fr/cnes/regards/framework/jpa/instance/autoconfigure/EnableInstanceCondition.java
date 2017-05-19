/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.autoconfigure;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 *
 * Class DisableInstanceCondition
 *
 * Spring condition to enable instanceEntity filter for JPA entities and reposiroties
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class EnableInstanceCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext pArg0, final AnnotatedTypeMetadata pArg1) {
        final String result = pArg0.getEnvironment()
                .getProperty("regards.jpa.instance.disableInstanceEntityAnnotation");
        return !Boolean.parseBoolean(result);
    }

}
