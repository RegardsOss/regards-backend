/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.configuration.dao.IUILayoutRepository;
import fr.cnes.regards.modules.configuration.domain.LayoutDefaultApplicationIds;
import fr.cnes.regards.modules.configuration.domain.UILayout;
import fr.cnes.regards.modules.configuration.service.exception.InitUIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Class LayoutService
 * <p>
 * Service to manage Project IHM Layouts configurations
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Service
@RegardsTransactional
public class UILayoutService extends AbstractUiConfigurationService implements IUILayoutService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(UILayoutService.class);

    @Autowired
    private IUILayoutRepository repository;

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
            final UILayout UILayout = new UILayout();
            UILayout.setApplicationId(LayoutDefaultApplicationIds.PORTAL.toString());
            UILayout.setLayout(layoutConf);
            if (!repository.findByApplicationId(UILayout.getApplicationId()).isPresent()) {
                repository.save(UILayout);
            }
        } catch (final IOException e) {
            throw new InitUIException(e);
        }
    }

    @Override
    public void initProjectUI(final String tenant) {
        try {
            final String layoutConf = readDefaultFileResource(defaultUserApplicationLayoutResource);
            final UILayout UILayout = new UILayout();
            UILayout.setApplicationId(LayoutDefaultApplicationIds.USER.toString());
            UILayout.setLayout(layoutConf);
            if (!repository.findByApplicationId(UILayout.getApplicationId()).isPresent()) {
                repository.save(UILayout);
            }
        } catch (final IOException e) {
            throw new InitUIException(e);
        }
    }

    @Override
    public UILayout retrieveLayout(final String applicationId) throws EntityNotFoundException {
        return repository.findByApplicationId(applicationId)
                         .orElseThrow(() -> new EntityNotFoundException(applicationId, UILayout.class));
    }

    @Override
    public UILayout saveLayout(final UILayout pUILayout) throws EntityException {
        if (repository.findByApplicationId(pUILayout.getApplicationId()).isPresent()) {
            throw new EntityAlreadyExistsException(pUILayout.getApplicationId());
        }
        // Check layout json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pUILayout.getLayout(), Object.class);
        } catch (RuntimeException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Layout is not a valid json format.", e);
        }
        return repository.save(pUILayout);
    }

    @Override
    public UILayout updateLayout(final UILayout UILayout) throws EntityException {

        // Check layout json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(UILayout.getLayout(), Object.class);
        } catch (final RuntimeException e) {
            LOG.error(e.getMessage(), e);
            throw new EntityInvalidException("Layout is not a valid json format.", e);
        }
        if (!repository.findByApplicationId(UILayout.getApplicationId()).isPresent()) {
            throw new EntityNotFoundException(UILayout.getId(), UILayout.class);
        }
        return repository.save(UILayout);
    }

}
