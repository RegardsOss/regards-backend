package fr.cnes.regards.modules.dam.dao.entities;

import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.DeletedEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Repository for deleted entities
 *
 * @author oroussel
 */
public interface IDeletedEntityRepository extends CrudRepository<DeletedEntity, Long> {

    Optional<DeletedEntity> findOneByIpId(UniformResourceName ipId);
}
