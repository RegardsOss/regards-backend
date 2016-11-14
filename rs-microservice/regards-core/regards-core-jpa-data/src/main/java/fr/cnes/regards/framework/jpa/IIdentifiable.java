/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa;

import java.io.Serializable;

/**
 * @param <I>
 *            identifier type
 * @author msordi
 *
 */
@FunctionalInterface
public interface IIdentifiable<I extends Serializable> {

    I getId();

    default boolean isIdentifiable() {
        return getId() != null;
    }
}
