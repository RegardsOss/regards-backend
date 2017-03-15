/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.IModuleRepository;
import fr.cnes.regards.modules.configuration.domain.Module;

/**
 *
 * Class ModuleService
 *
 * Service to manage modules entities
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Service(value = "moduleService")
@Transactional
public class ModuleService implements IModuleService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ModuleService.class);

    @Autowired
    private IModuleRepository repository;

    @Override
    public Module retrieveModule(final Long pModuleId) throws EntityNotFoundException {
        final Module module = repository.findOne(pModuleId);
        if (module == null) {
            throw new EntityNotFoundException(pModuleId, Module.class);
        }
        return module;
    }

    @Override
    public Page<Module> retrieveModules(final String pApplicationId, final Pageable pPageable) {
        return repository.findByApplicationId(pApplicationId, pPageable);
    }

    @Override
    public Page<Module> retrieveActiveModules(final String pApplicationId, final Pageable pPageable) {
        return repository.findByApplicationIdAndActiveTrue(pApplicationId, pPageable);
    }

    @Override
    public Module saveModule(final Module pModule) throws EntityInvalidException {
        // Check module configuration json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pModule.getConf(), Object.class);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Layout is not a valid json format.");
        }
        if (pModule.isDefaultDynamicModule()) {
            disableDefaultForAllApplicationModules(pModule.getApplicationId());
        }
        return repository.save(pModule);
    }

    @Override
    public Module updateModule(final Module pModule) throws EntityNotFoundException, EntityInvalidException {
        // Check layut json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pModule.getConf(), Object.class);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Layout is not a valid json format.");
        }
        if (!repository.exists(pModule.getId())) {
            throw new EntityNotFoundException(pModule.getId(), Module.class);
        }
        if (pModule.isDefaultDynamicModule()) {
            disableDefaultForAllApplicationModules(pModule.getApplicationId());
        }
        return repository.save(pModule);
    }

    @Override
    public void deleteModule(final Long pModuleId) throws EntityNotFoundException {
        if (!repository.exists(pModuleId)) {
            throw new EntityNotFoundException(pModuleId, Module.class);
        }
        repository.delete(pModuleId);

    }

    /**
     *
     * Set to false the defaultDynamicModule attribute of all modules for the given application id
     *
     * @param pApplicationId
     * @since 1.0-SNAPSHOT
     */
    private void disableDefaultForAllApplicationModules(final String pApplicationId) {
        final List<Module> modules = repository.findByApplicationIdAndDefaultDynamicModuleTrue(pApplicationId);
        for (final Module module : modules) {
            module.setDefaultDynamicModule(false);
            repository.save(module);
        }
    }

}
