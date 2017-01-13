/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.modules.storage.domain.DataObject;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public interface DataObjectRepository extends JpaRepository<DataObject, Long> {

}
