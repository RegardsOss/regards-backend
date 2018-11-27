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
package fr.cnes.regards.framework.modules.plugins.dao;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;

/***
 * Unit testing of {@link PluginParameter} persistence.
 *
 * @author Christophe Mertz
 *
 */

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PluginDaoTestConfig.class })
public class PluginParameterIT extends PluginDaoUtility {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginParameterIT.class);

    @Before
    public void before() {
        injectToken(PROJECT);
        cleanDb();
    }

    /**
     * Unit testing for the creation of a {@link PluginParameter}
     */
    @Test
    public void createPluginParameter() {
        final long nPluginParameter = paramRepository.count();

        paramRepository.save(ONE_PARAMETER);
        paramRepository.saveAll(LIST_PARAMETERS);

        Assert.assertEquals(nPluginParameter + 1 + LIST_PARAMETERS.size(), paramRepository.count());

        plgRepository.deleteAll();
    }

    /**
     * Unit testing for the update of a {@link PluginParameter}
     */
    @Test
    public void updatePluginParameter() {
        paramRepository.save(INTERFACEPARAMETERS.stream().findFirst().get());
        PluginParameter paramJpa = paramRepository.save(LIST_PARAMETERS.stream().findFirst().get());
        Assert.assertEquals(paramJpa.getName(), LIST_PARAMETERS.stream().findFirst().get().getName());

        paramRepository.findAll().forEach(p -> LOGGER.info(p.getName()));

        paramRepository.save(paramJpa);

        Optional<PluginParameter> foundParamOpt = paramRepository.findById(paramJpa.getId());
        Assert.assertTrue(foundParamOpt.isPresent());
        Assert.assertEquals(foundParamOpt.get().getName(), paramJpa.getName());

        paramRepository.deleteAll();
        plgRepository.deleteAll();
    }

    /**
     * Unit testing for the delete of a {@link PluginParameter}
     */
    @Test
    public void deletePluginParameter() {
        PluginParameter paramJpa = paramRepository.save(ONE_PARAMETER);
        paramRepository.saveAll(LIST_PARAMETERS);
        Assert.assertEquals(1 + LIST_PARAMETERS.size(), paramRepository.count());

        // Delete a plugin parameter
        paramRepository.delete(paramJpa);
        Assert.assertEquals(LIST_PARAMETERS.size(), paramRepository.count());

        // Delete a plugin parameter
        paramRepository.delete(LIST_PARAMETERS.stream().findFirst().get());
        Assert.assertEquals(LIST_PARAMETERS.size() - 1, paramRepository.count());
    }

    /**
     * Unit testing about the dynamic values of a {@link PluginParameter}
     */
    @Test
    public void createAndFindPluginParameter() {
        // first plugin parameter
        PluginParameter savedParam = paramRepository.save(ONE_PARAMETER);
        Assert.assertNotNull(savedParam.getId());
        Assert.assertEquals(1, paramRepository.count());

        // second plugin parameter with dynamic values
        PluginParameter paramWithDynValues = paramRepository.save(LIST_PARAMETERS.stream().findFirst().get());
        Assert.assertNotNull(paramWithDynValues.getId());
        Assert.assertEquals(2, paramRepository.count());

        // search the first plugin parameter
        Optional<PluginParameter> paramFound2Opt = paramRepository.findById(savedParam.getId());
        Assert.assertTrue(paramFound2Opt.isPresent());
        PluginParameter paramFound2 = paramFound2Opt.get();
        Assert.assertEquals(savedParam.isDynamic(), paramFound2.isDynamic());
        Assert.assertTrue(savedParam.getDynamicsValues().isEmpty());
        Assert.assertEquals(savedParam.getName(), paramFound2.getName());
        Assert.assertEquals(savedParam.getValue(), paramFound2.getValue());
        Assert.assertEquals(savedParam.getId(), paramFound2.getId());
    }

    /**
     * Unit testing about the dynamic values of a {@link PluginParameter}
     */
    @Test
    public void controlPluginParameterDynamicValues() {
        // first plugin parameter
        final PluginParameter savedParam = paramRepository.save(ONE_PARAMETER);
        Assert.assertNotNull(savedParam.getId());
        Assert.assertEquals(1, paramRepository.count());

        // second plugin parameter with dynamic values
        PluginParameter dynParam = LIST_PARAMETERS.stream().filter(p -> p.getName().equals("param-dyn21")).findFirst()
                .get();
        final PluginParameter paramWithDynValues = paramRepository.save(dynParam);
        Assert.assertNotNull(paramWithDynValues.getId());
        Assert.assertEquals(2, paramRepository.count());

        // search the second plugin parameter
        final PluginParameter paramFound = paramRepository.findOneWithDynamicsValues(paramWithDynValues.getId());
        Assert.assertNotNull(paramFound);
        paramFound.getDynamicsValues().stream().forEach(p -> LOGGER.info(p.getValue()));

        // test dynamics values of the second parameter
        Assert.assertEquals(paramWithDynValues.isDynamic(), paramFound.isDynamic());
        Assert.assertEquals(paramWithDynValues.getDynamicsValues().size(), paramFound.getDynamicsValues().size());
        Assert.assertEquals(paramWithDynValues.getName(), paramFound.getName());
        Assert.assertEquals(paramWithDynValues.getValue(), paramFound.getValue());
        Assert.assertEquals(paramWithDynValues.getId(), paramFound.getId());
        Assert.assertEquals(paramWithDynValues.getDynamicsValuesAsString().size(),
                            paramFound.getDynamicsValuesAsString().size());
        paramWithDynValues.getDynamicsValuesAsString().stream()
                .forEach(s -> paramFound.getDynamicsValuesAsString().contains(s));
    }

}
