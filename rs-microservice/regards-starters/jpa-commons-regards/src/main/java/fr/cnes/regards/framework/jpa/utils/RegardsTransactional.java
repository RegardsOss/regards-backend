package fr.cnes.regards.framework.jpa.utils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Transactional override to configure rollbackFor
 * @author Sylvain VISSIERE-GUERINET
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional
public @interface RegardsTransactional {

    @AliasFor(annotation = Transactional.class) String transactionManager() default "";

    /**
     * @see Transactional#rollbackFor()
     * Defaults to ModuleException and runtimes
     */
    @AliasFor(annotation = Transactional.class) Class<? extends Throwable>[] rollbackFor() default {
            ModuleException.class };

}
