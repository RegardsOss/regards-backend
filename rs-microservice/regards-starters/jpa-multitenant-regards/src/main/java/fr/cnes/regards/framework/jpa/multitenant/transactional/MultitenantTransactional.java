/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.transactional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;

/**
 * Meta annotation to manage multitenant transaction
 *
 * @author msordi
 *
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional(transactionManager = MultitenantDaoProperties.MULTITENANT_TRANSACTION_MANAGER)
public @interface MultitenantTransactional {

    /**
     * The transaction propagation type.
     * <p>
     * Defaults to {@link Propagation#REQUIRED}.
     * 
     * @see org.springframework.transaction.interceptor.TransactionAttribute#getPropagationBehavior()
     */
    @AliasFor(annotation = Transactional.class)
    Propagation propagation() default Propagation.REQUIRED;

}
