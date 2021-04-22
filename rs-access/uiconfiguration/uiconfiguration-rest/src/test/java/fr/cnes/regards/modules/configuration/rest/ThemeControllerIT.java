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
package fr.cnes.regards.modules.configuration.rest;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.configuration.dao.IThemeRepository;
import fr.cnes.regards.modules.configuration.domain.Theme;

/**
 *
 * Class InstanceLayoutControllerIT
 *
 * IT Tests for REST Controller
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class ThemeControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ThemeControllerIT.class);

    @Autowired
    private IThemeRepository repository;

    private Theme theme;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    private Theme createTheme(final boolean pActive, final String pName) {
        final Theme theme = new Theme();
        theme.setActive(pActive);
        theme.setConfiguration("{}");
        theme.setName(pName);
        return theme;
    }

    @Before
    public void init() {
        theme = createTheme(false, "Theme");
        final Theme theme2 = createTheme(true, "Theme2");
        final Theme theme3 = createTheme(false, "Theme3");
        theme = repository.save(theme);
        repository.save(theme2);
        repository.save(theme3);
    }

    /**
     *
     * Test retrieve all themes
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testGetAllThemes() {
        performDefaultGet(ThemeController.ROOT_MAPPING,
                          customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 7),
                          "Error getting all themes. There should 7 themes. The 4 default ones and the 3 created in this test.");
    }

    /**
     * Test to retrieve one theme
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testGetOneTheme() {
        performDefaultGet(ThemeController.ROOT_MAPPING + ThemeController.THEME_ID_MAPPING,
                          customizer().expectStatusOk(), "Error getting one theme", theme.getId());
    }

    /**
     * Test to delete one theme
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testDeleteOneTheme() {

        performDefaultDelete(ThemeController.ROOT_MAPPING + ThemeController.THEME_ID_MAPPING,
                             customizer().expectStatusOk(), "Error deleting one theme", theme.getId());

        performDefaultGet(ThemeController.ROOT_MAPPING + ThemeController.THEME_ID_MAPPING,
                          customizer().expectStatusNotFound(), "The deleted theme should not pe present anymore",
                          theme.getId());
    }

    /**
     * Test to save a new theme
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testSaveTheme() {
        final Theme theme = createTheme(true, "NewTheme");
        performDefaultPost(ThemeController.ROOT_MAPPING, theme, customizer().expectStatusOk(),
                           "Error saving new theme");

        performDefaultGet(ThemeController.ROOT_MAPPING,
                          customizer().expectStatusOk().expectToHaveSize(JSON_PATH_CONTENT, 8),
                          "There should be the 7 initial themes and the new created one.");
    }

    /**
     * Test to update a theme
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testUpdateTheme() {
        theme.setActive(true);
        performDefaultPut(ThemeController.ROOT_MAPPING + ThemeController.THEME_ID_MAPPING, theme,
                          customizer().expectStatusOk(), "Error saving new theme", theme.getId());
    }

}
