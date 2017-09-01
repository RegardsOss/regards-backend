/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.multitenant.event.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
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
@Service(value = "themeService")
@RegardsTransactional
public class ThemeService extends AbstractUiConfigurationService
        implements IThemeService, ApplicationListener<ApplicationReadyEvent> {

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

    @Autowired
    private IThemeRepository repository;

    /**
     * Perform initialization only when the whole application is ready
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        // Initialize subscriber for new tenant connection and initialize database if not already done
        getInstanceSubscriber().subscribeTo(TenantConnectionReady.class, new TenantConnectionReadyEventHandler());
    }

    private class TenantConnectionReadyEventHandler implements IHandler<TenantConnectionReady> {

        @Override
        public void handle(final TenantWrapper<TenantConnectionReady> pWrapper) {
            if (getMicroserviceName().equals(pWrapper.getContent().getMicroserviceName())) {
                getRuntimeTenantResolver().forceTenant(pWrapper.getContent().getTenant());
                initProjectUI(pWrapper.getContent().getTenant());
            }
        }

    }

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
    public Theme updateTheme(final Theme pTheme) throws EntityException {
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
    }

    @Override
    protected void initInstanceUI() {
        // Initialize with the same default theme as projects
        this.initProjectUI(null);
    }

}
