package fr.cnes.regards.modules.storage.service.parameter;

import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.storage.domain.parameter.StorageParameter;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IStorageParameterService {

    static final String DEFAULT_UPDATE_RATE = "120";

    StorageParameter create(StorageParameter storageParameter) throws EntityAlreadyExistsException;

    StorageParameter retrieveByName(String parameterName) throws EntityNotFoundException;

    StorageParameter update(Long toUpdateId, StorageParameter updated)
            throws EntityNotFoundException, EntityInconsistentIdentifierException;

    void delete(String parameterName);

    List<StorageParameter> retrieveAll();
}
