/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessrights.dao.projects.IAccessSettingsRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;

/**
 * {@link IAccessSettingsService} implementation
 *
 * @author CS SI
 */
@Service
public class AccessSettingsService implements IAccessSettingsService {

    /**
     * CRUD repository managing access settings. Autowired by Spring.
     */
    private final IAccessSettingsRepository accessSettingsRepository;

    /**
     * Creates an {@link AccessSettingsService} wired to the given {@link IProjectUserRepository}.
     *
     * @param pAccessSettingsRepository
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public AccessSettingsService(final IAccessSettingsRepository pAccessSettingsRepository) {
        super();
        accessSettingsRepository = pAccessSettingsRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.IAccessSettingsService#retrieve()
     */
    @Override
    public AccessSettings retrieve() {
        return accessSettingsRepository.findAll().get(0);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.IAccessSettingsService#update()
     */
    @Override
    public AccessSettings update(final AccessSettings pAccessSettings) throws EntityNotFoundException {
        if (!accessSettingsRepository.exists(pAccessSettings.getId())) {
            throw new EntityNotFoundException(pAccessSettings.getId().toString(), AccessSettings.class);
        }
        return accessSettingsRepository.save(pAccessSettings);
    }

}
