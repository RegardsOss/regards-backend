/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.transactional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.instance.properties.InstanceDaoProperties;

/**
 * Meta annotation to manage instance transaction
 *
 * @author Sébastien Binda
 *
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional(transactionManager = InstanceDaoProperties.INSTANCE_TRANSACTION_MANAGER)
public @interface InstanceTransactional {

}
