/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.IThemeRepository;
import fr.cnes.regards.modules.configuration.domain.Theme;
import fr.cnes.regards.modules.configuration.service.exception.InitUIException;

@Service(value = "themeService")
@Transactional
public class ThemeService extends AbstractUiConfigurationService implements IThemeService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ModuleService.class);

    /**
     * The default configuration for dark theme
     */
    @Value("classpath:DefaultDarkTheme.json")
    private Resource defaultDarkThemeResource;

    /**
     * The default configuration for light theme
     */
    @Value("classpath:DefaultLightTheme.json")
    private Resource defaultLightThemeResource;

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

    @Override
    protected void initProjectUI(final String pTenant) {
        final List<Theme> themes = repository.findAll();
        if ((themes == null) || themes.isEmpty()) {
            final Theme defaultTheme = new Theme();
            defaultTheme.setName("Dark");
            defaultTheme.setActive(true);
            try {
                defaultTheme.setConfiguration(readDefaultFileResource(defaultDarkThemeResource));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                throw new InitUIException(e);
            }
            repository.save(defaultTheme);

            final Theme defaultLightTheme = new Theme();
            defaultLightTheme.setName("Light");
            defaultLightTheme.setActive(false);
            try {
                defaultLightTheme.setConfiguration(readDefaultFileResource(defaultLightThemeResource));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                throw new InitUIException(e);
            }
            repository.save(defaultLightTheme);
        }

    }

    @Override
    protected void initInstanceUI() {
        // Initialize with the same default theme as projects
        this.initProjectUI(null);
    }

}
