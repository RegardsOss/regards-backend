/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.ILayoutRepository;
import fr.cnes.regards.modules.configuration.domain.Layout;
import fr.cnes.regards.modules.configuration.domain.LayoutDefaultApplicationIds;
import fr.cnes.regards.modules.configuration.service.exception.InitUIException;

/**
 *
 * Class LayoutService
 *
 * Service to manage Project IHM Layouts configurations
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Service
@RegardsTransactional
public class LayoutService extends AbstractUiConfigurationService implements ILayoutService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(LayoutService.class);

    @Autowired
    private ILayoutRepository repository;

    /**
     * The email validation template as html
     */
    @Value("classpath:DefaultUserApplicationLayout.json")
    private Resource defaultUserApplicationLayoutResource;

    /**
     * The email validation template as html
     */
    @Value("classpath:DefaultPortalApplicationLayout.json")
    private Resource defaultPortalApplicationLayoutResource;

    @Override
    public void initInstanceUI() {
        try {
            final String layoutConf = readDefaultFileResource(defaultPortalApplicationLayoutResource);
            final Layout layout = new Layout();
            layout.setApplicationId(LayoutDefaultApplicationIds.PORTAL.toString());
            layout.setLayout(layoutConf);
            if (!repository.findByApplicationId(layout.getApplicationId()).isPresent()) {
                repository.save(layout);
            }
        } catch (final IOException e) {
            throw new InitUIException(e);
        }
    }

    @Override
    public void initProjectUI(final String pTenant) {
        try {
            final String layoutConf = readDefaultFileResource(defaultUserApplicationLayoutResource);
            final Layout layout = new Layout();
            layout.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
            layout.setLayout(layoutConf);
            if (!repository.findByApplicationId(layout.getApplicationId()).isPresent()) {
                repository.save(layout);
            }
        } catch (final IOException e) {
            throw new InitUIException(e);
        }
    }

    @Override
    public Layout retrieveLayout(final String pApplicationId) throws EntityNotFoundException {
        return repository.findByApplicationId(pApplicationId)
                .orElseThrow(() -> new EntityNotFoundException(pApplicationId, Layout.class));
    }

    @Override
    public Layout saveLayout(final Layout pLayout) throws EntityException {
        if (repository.findByApplicationId(pLayout.getApplicationId()).isPresent()) {
            throw new EntityAlreadyExistsException(pLayout.getApplicationId());
        }
        // Check layout json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pLayout.getLayout(), Object.class);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Layout is not a valid json format.", e);
        }
        return repository.save(pLayout);
    }

    @Override
    public Layout updateLayout(final Layout pLayout) throws EntityException {

        // Check layout json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pLayout.getLayout(), Object.class);
        } catch (final RuntimeException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Layout is not a valid json format.", e);
        }
        if (!repository.findByApplicationId(pLayout.getApplicationId()).isPresent()) {
            throw new EntityNotFoundException(pLayout.getId(), Layout.class);
        }
        return repository.save(pLayout);
    }

}
