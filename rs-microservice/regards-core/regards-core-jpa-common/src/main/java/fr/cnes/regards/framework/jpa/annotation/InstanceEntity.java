/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Class InstanceEntity
 *
 * Annotation to indicates that the entity is associated to the instance database. Used to separate multitenancy
 * projects databases and instance database.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InstanceEntity {
}
