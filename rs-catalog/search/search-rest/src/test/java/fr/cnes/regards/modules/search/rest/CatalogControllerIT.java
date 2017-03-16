/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;

/**
 * Integration tests for {@link CatalogController}
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@TestPropertySource(locations = { "classpath:test.properties" })
@MultitenantTransactional
public class CatalogControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CatalogControllerIT.class);

    // @Autowired
    // private IThemeRepository repository;
    //
    // private Theme theme;

    // private Theme createTheme(final boolean pActive) {
    // final Theme theme = new Theme();
    // theme.setActive(pActive);
    // theme.setConfiguration("{}");
    // theme.setName("Test");
    // return theme;
    // }

    @Before
    public void init() {
        // theme = createTheme(false);
        // final Theme theme2 = createTheme(true);
        // final Theme theme3 = createTheme(false);
        // repository.save(theme);
        // repository.save(theme2);
        // repository.save(theme3);
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

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}