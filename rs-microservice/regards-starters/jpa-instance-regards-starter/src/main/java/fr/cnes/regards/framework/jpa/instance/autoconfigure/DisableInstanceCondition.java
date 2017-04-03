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
 * Spring condition to disable instanceEntity filter for JPA entities and reposiroties
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class DisableInstanceCondition implements Condition {

    @Override
    public boolean matches(final ConditionContext pArg0, final AnnotatedTypeMetadata pArg1) {
        final String result = pArg0.getEnvironment()
                .getProperty("regards.jpa.instance.disableInstanceEntityAnnotation");
        final boolean bool = Boolean.parseBoolean(result);
        return bool == true;
    }

}
