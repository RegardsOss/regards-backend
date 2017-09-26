/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.parameter;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.storage.dao.IStorageParameterRepository;
import fr.cnes.regards.modules.storage.domain.parameter.StorageParameter;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
public class StorageParameterService implements IStorageParameterService, ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private IStorageParameterRepository repository;

    @Override
    public StorageParameter create(StorageParameter storageParameter) throws EntityAlreadyExistsException {
        if (repository.findOneByName(storageParameter.getName()) != null) {
            throw new EntityAlreadyExistsException(
                    "Parameter with name " + storageParameter.getName() + " already exists!");
        }
        return repository.save(storageParameter);
    }

    @Override
    public StorageParameter retrieveByName(String parameterName) throws EntityNotFoundException {
        StorageParameter result = repository.findOneByName(parameterName);
        if (result == null) {
            throw new EntityNotFoundException(parameterName, StorageParameter.class);
        }
        return result;
    }

    @Override
    public StorageParameter update(Long toUpdateId, StorageParameter updated)
            throws EntityNotFoundException, EntityInconsistentIdentifierException {
        StorageParameter fromDB = repository.findOneByName(updated.getName());
        if (fromDB == null) {
            throw new EntityNotFoundException(updated.getName(), StorageParameter.class);
        }
        if (!fromDB.getId().equals(toUpdateId)) {
            throw new EntityInconsistentIdentifierException(toUpdateId, fromDB.getId(), StorageParameter.class);
        }
        return repository.save(updated);
    }

    @Override
    public void delete(String parameterName) {
        StorageParameter toDelete = repository.findOneByName(parameterName);
        if (toDelete != null) {
            repository.delete(toDelete.getId());
        }
    }

    @Override
    public List<StorageParameter> retrieveAll() {
        return repository.findAll();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        initDefaultParameters();
    }

    private void initDefaultParameters() {
        if (repository.findOneByName(StorageParameter.UPDATE_RATE) == null) {
            try {
                create(new StorageParameter(StorageParameter.UPDATE_RATE, DEFAULT_UPDATE_RATE));
            } catch (EntityAlreadyExistsException e) {
                //cannot happen as we are checking that the parameter does not already exists
            }
        }
    }
}
