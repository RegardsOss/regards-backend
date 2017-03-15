/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

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

    private Theme createTheme(final boolean pActive) {
        final Theme theme = new Theme();
        theme.setActive(pActive);
        theme.setConfiguration("{}");
        theme.setName("Test");
        return theme;
    }

    @Before
    public void init() {
        theme = createTheme(false);
        final Theme theme2 = createTheme(true);
        final Theme theme3 = createTheme(false);
        repository.save(theme);
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
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(3)));
        performDefaultGet("/themes", expectations, "Error getting all themes");
    }

    /**
     * Test to retrieve one theme
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testGetOneTheme() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultGet("/themes/{themeId}", expectations, "Error getting one theme", theme.getId());
    }

    /**
     * Test to delete one theme
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testDeleteOneTheme() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultDelete("/themes/{themeId}", expectations, "Error deleting one theme", theme.getId());

        expectations.clear();
        expectations.add(status().isNotFound());
        performDefaultGet("/themes/{themeId}", expectations, "Plop", theme.getId());
    }

    /**
     * Test to save a new theme
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testSaveTheme() {
        final Theme theme = createTheme(true);
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultPost("/themes", theme, expectations, "Error saving new theme");

        expectations.clear();
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(4)));
        performDefaultGet("/themes", expectations, "Error getting all themes");
    }

    /**
     * Test to update a theme
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testUpdateTheme() {
        theme.setActive(true);
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performDefaultPut("/themes/{themeId}", theme, expectations, "Error saving new theme", theme.getId());
    }

}
