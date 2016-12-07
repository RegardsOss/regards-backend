/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.projects.IAccessSettingsRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;

/**
 * {@link IAccessSettingsService} implementation
 *
 * @author Xavier-Alexandre Brochard
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
     * @see fr.cnes.regards.modules.accessrights.service.role.IAccessSettingsService#retrieve()
     */
    @Override
    public AccessSettings retrieve() throws EntityNotFoundException {
        final List<AccessSettings> list = accessSettingsRepository.findAll();
        if (list.isEmpty()) {
            throw new EntityNotFoundException(0L, AccessSettings.class);
        }
        return list.get(0);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.role.IAccessSettingsService#update()
     */
    @Override
    public AccessSettings update(final AccessSettings pAccessSettings) throws EntityNotFoundException {
        if (!accessSettingsRepository.exists(pAccessSettings.getId())) {
            throw new EntityNotFoundException(pAccessSettings.getId().toString(), AccessSettings.class);
        }
        return accessSettingsRepository.save(pAccessSettings);
    }

}
