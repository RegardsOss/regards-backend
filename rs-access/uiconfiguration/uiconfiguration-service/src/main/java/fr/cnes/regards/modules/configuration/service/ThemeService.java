/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.configuration.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.IThemeRepository;
import fr.cnes.regards.modules.configuration.domain.Theme;
import fr.cnes.regards.modules.configuration.service.exception.InitUIException;

/**
 * Service managing themes
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
@Service
@RegardsTransactional
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

    /**
     * The default configuration for energy theme
     */
    @Value("classpath:DefaultEnergyTheme.json")
    private Resource defaultEnergyThemeResource;

    /**
     * The default configuration for ocean theme
     */
    @Value("classpath:DefaultOceanTheme.json")
    private Resource defaultOceanThemeResource;

    @Autowired
    private IThemeRepository repository;

    @Override
    public Theme retrieveTheme(final Long themeId) throws EntityNotFoundException {
        final Optional<Theme> theme = repository.findById(themeId);
        if (!theme.isPresent()) {
            throw new EntityNotFoundException(themeId, Theme.class);
        }
        return theme.get();
    }

    @Override
    public Page<Theme> retrieveThemes(final Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Theme saveTheme(final Theme theme) {
        // If new theme is the only one active theme, so first disable all other themes
        if (theme.isActive()) {
            disableAllActiveThemes();
        }
        return repository.save(theme);
    }

    @Override
    public Theme updateTheme(final Theme theme) throws EntityException {
        // Check theme existence
        if (!repository.existsById(theme.getId())) {
            throw new EntityNotFoundException(theme.getId(), Theme.class);
        }

        // If theme is the only one active theme, so first disable all other themes
        if (theme.isActive()) {
            disableAllActiveThemes();
        }
        return repository.save(theme);
    }

    @Override
    public void deleteTheme(final Long themeId) throws EntityNotFoundException {
        // Check theme existence
        if (!repository.existsById(themeId)) {
            throw new EntityNotFoundException(themeId, Theme.class);
        }

        repository.deleteById(themeId);
    }

    @Override
    public List<Theme> retrieveAllThemes() {
        return repository.findAll();
    }

    @Override
    public Optional<Theme> retrieveByName(String name) {
        return repository.findByName(name);
    }

    /**
     *
     * Set to false the defaultDynamicModule attribute of all modules for the given application id
     *
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
    protected void initProjectUI(final String tenant) {
        if (!repository.findByName("Dark").isPresent()) {
            Theme defaultTheme = new Theme();
            defaultTheme.setName("Dark");
            defaultTheme.setActive(true);
            try {
                defaultTheme.setConfiguration(readDefaultFileResource(defaultDarkThemeResource));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                throw new InitUIException(e);
            }
            repository.save(defaultTheme);
        }

        if (!repository.findByName("Light").isPresent()) {
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

        if (!repository.findByName("Energy").isPresent()) {
            final Theme defaultEnergyTheme = new Theme();
            defaultEnergyTheme.setName("Energy");
            defaultEnergyTheme.setActive(false);
            try {
                defaultEnergyTheme.setConfiguration(readDefaultFileResource(defaultEnergyThemeResource));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                throw new InitUIException(e);
            }
            repository.save(defaultEnergyTheme);
        }

        if (!repository.findByName("Ocean").isPresent()) {
            final Theme defaultOceanTheme = new Theme();
            defaultOceanTheme.setName("Ocean");
            defaultOceanTheme.setActive(false);
            try {
                defaultOceanTheme.setConfiguration(readDefaultFileResource(defaultOceanThemeResource));
            } catch (final IOException e) {
                LOG.error(e.getMessage(), e);
                throw new InitUIException(e);
            }
            repository.save(defaultOceanTheme);
        }
    }

    @Override
    protected void initInstanceUI() {
        // Initialize with the same default theme as projects
        this.initProjectUI(null);
    }

}
