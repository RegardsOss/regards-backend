/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.instance.transactional;

import java.lang.annotation.*;

import fr.cnes.regards.framework.jpa.instance.properties.InstanceDaoProperties;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;

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
@RegardsTransactional(transactionManager = InstanceDaoProperties.INSTANCE_TRANSACTION_MANAGER)
public @interface InstanceTransactional {

}
