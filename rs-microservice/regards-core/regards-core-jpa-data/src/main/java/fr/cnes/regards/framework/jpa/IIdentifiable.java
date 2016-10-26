/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa;

import java.io.Serializable;

/**
 * @param <ID>
 *            identifier type
 * @author msordi
 *
 */
public interface IIdentifiable<ID extends Serializable> {

    ID getId();

}
