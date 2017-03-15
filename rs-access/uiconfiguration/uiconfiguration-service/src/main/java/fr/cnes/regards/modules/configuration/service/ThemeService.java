/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.IThemeRepository;
import fr.cnes.regards.modules.configuration.domain.Theme;

@Service(value = "themeService")
public class ThemeService implements IThemeService {

    @Autowired
    private IThemeRepository repository;

    @Override
    public Theme retrieveTheme(final Long pThemeId) throws EntityNotFoundException {
        final Theme theme = repository.findOne(pThemeId);
        if (theme == null) {
            throw new EntityNotFoundException(pThemeId, Theme.class);
        }
        return theme;
    }

    @Override
    public Page<Theme> retrieveThemes(final Pageable pPageable) {
        return repository.findAll(pPageable);
    }

    @Override
    public Theme saveTheme(final Theme pTheme) throws EntityInvalidException {
        // If new theme is the only one active theme, so first disable all other themes
        if (pTheme.isActive()) {
            disableAllActiveThemes();
        }
        return repository.save(pTheme);
    }

    @Override
    public Theme updateTheme(final Theme pTheme) throws EntityNotFoundException, EntityInvalidException {
        // Check theme existence
        if (!repository.exists(pTheme.getId())) {
            throw new EntityNotFoundException(pTheme.getId(), Theme.class);
        }

        // If theme is the only one active theme, so first disable all other themes
        if (pTheme.isActive()) {
            disableAllActiveThemes();
        }
        return repository.save(pTheme);
    }

    @Override
    public void deleteTheme(final Long pThemeId) throws EntityNotFoundException {
        // Check theme existence
        if (!repository.exists(pThemeId)) {
            throw new EntityNotFoundException(pThemeId, Theme.class);
        }

        repository.delete(pThemeId);
    }

    /**
     *
     * Set to false the defaultDynamicModule attribute of all modules for the given application id
     *
     * @param pApplicationId
     * @since 1.0-SNAPSHOT
     */
    private void disableAllActiveThemes() {
        final List<Theme> themes = repository.findByActiveTrue();
        for (final Theme theme : themes) {
            theme.setActive(false);
            repository.save(theme);
        }
    }

}
