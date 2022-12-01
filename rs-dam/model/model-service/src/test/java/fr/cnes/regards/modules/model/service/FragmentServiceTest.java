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
package fr.cnes.regards.modules.model.service;

import com.google.common.collect.Iterables;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.model.dao.IAttributeModelRepository;
import fr.cnes.regards.modules.model.dao.IFragmentRepository;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModelBuilder;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.domain.attributes.restriction.EnumerationRestriction;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Test fragment service
 *
 * @author Marc Sordi
 */
@RunWith(MockitoJUnitRunner.class)
public class FragmentServiceTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FragmentServiceTest.class);

    /**
     * Test fragment name
     */
    private static final String TEST_FRAG_NAME = "FRAG";

    /**
     * Test fragment name
     */
    private static final String TEST_FRAG_DESC = "Test fragment";

    /**
     * Fragment repository
     */
    @Mock
    private IFragmentRepository mockFragmentRepository;

    /**
     * Attribute model repository
     */
    @Mock
    private IAttributeModelRepository mockAttModelRepository;

    /**
     * Attribute model service
     */
    @Mock
    private IAttributeModelService mockAttModelSrv;

    /**
     * List argument captor
     */
    @Captor
    private ArgumentCaptor<Iterable<AttributeModel>> attModelCaptor;

    /**
     * Mocked fragment service
     */
    private IFragmentService fragmentSrv;

    @Before
    public void beforeTest() {
        fragmentSrv = new FragmentService(mockFragmentRepository,
                                          mockAttModelRepository,
                                          mockAttModelSrv,
                                          Mockito.mock(IPublisher.class));
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_MOD_020")
    @Purpose("Create a fragment")
    public void addFragmentTest() throws ModuleException {
        Fragment expected = Fragment.buildFragment(TEST_FRAG_NAME, TEST_FRAG_DESC);

        Mockito.when(mockFragmentRepository.findByName(TEST_FRAG_NAME)).thenReturn(null);
        // lets consider there is no attribute created yet
        Mockito.when(mockAttModelSrv.isFragmentCreatable(TEST_FRAG_NAME)).thenReturn(true);
        Mockito.when(mockFragmentRepository.save(expected)).thenReturn(expected);

        Fragment retrieved = fragmentSrv.addFragment(expected);
        Assert.assertNotNull(retrieved);
    }

    @Requirement("REGARDS_DSL_DAM_MOD_020")
    @Purpose("Manage conflicting fragment")
    @Test(expected = EntityAlreadyExistsException.class)
    public void addExistingFragmentTest() throws ModuleException {
        Fragment expected = Fragment.buildFragment(TEST_FRAG_NAME, TEST_FRAG_DESC);

        Mockito.when(mockFragmentRepository.findByName(TEST_FRAG_NAME)).thenReturn(expected);

        fragmentSrv.addFragment(expected);
    }

    @Test(expected = EntityNotFoundException.class)
    public void updateNotIdetnfiableFragmentTest() throws ModuleException {
        Long fragmentId = 1L;
        Fragment expected = Fragment.buildFragment(TEST_FRAG_NAME, TEST_FRAG_DESC);

        fragmentSrv.updateFragment(fragmentId, expected);
    }

    @Test(expected = EntityInconsistentIdentifierException.class)
    public void updateInconsistentFragmentTest() throws ModuleException {
        Long fragmentId = 1L;
        Fragment expected = Fragment.buildFragment(TEST_FRAG_NAME, TEST_FRAG_DESC);
        expected.setId(2L);

        fragmentSrv.updateFragment(fragmentId, expected);
    }

    @Test(expected = EntityNotFoundException.class)
    public void updateUnknownFragmentTest() throws ModuleException {
        Long fragmentId = 1L;
        Fragment expected = Fragment.buildFragment(TEST_FRAG_NAME, TEST_FRAG_DESC);
        expected.setId(fragmentId);

        Mockito.when(mockFragmentRepository.existsById(fragmentId)).thenReturn(false);

        fragmentSrv.updateFragment(fragmentId, expected);
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_MOD_020")
    @Purpose("Update a fragment")
    public void updateFragmentTest() throws ModuleException {
        Long fragmentId = 1L;
        Fragment expected = Fragment.buildFragment(TEST_FRAG_NAME, TEST_FRAG_DESC);
        expected.setId(fragmentId);

        Mockito.when(mockFragmentRepository.existsById(fragmentId)).thenReturn(true);
        Mockito.when(mockFragmentRepository.save(expected)).thenReturn(expected);

        Assert.assertNotNull(fragmentSrv.updateFragment(fragmentId, expected));
    }

    @Requirement("REGARDS_DSL_DAM_MOD_020")
    @Purpose("Manage fragment deletion")
    @Test(expected = EntityNotEmptyException.class)
    public void deleteNonEmptyFragment() throws ModuleException {
        Long fragmentId = 1L;
        List<AttributeModel> attModels = new ArrayList<>();
        attModels.add(new AttributeModelBuilder("MOCK", PropertyType.STRING, "ForTests").setNoRestriction().build());

        Mockito.when(mockAttModelRepository.findByFragmentId(fragmentId)).thenReturn(attModels);

        fragmentSrv.deleteFragment(fragmentId);
    }

    /**
     * Test fragment export
     *
     * @throws ModuleException if error occurs!
     */
    @Test
    public void exportFragmentTest() throws ModuleException {
        Long fragmentId = 1L;
        Fragment expected = Fragment.buildFragment(TEST_FRAG_NAME, TEST_FRAG_DESC);
        expected.setId(fragmentId);

        List<AttributeModel> attModels = new ArrayList<>();
        attModels.add(new AttributeModelBuilder("NAME", PropertyType.BOOLEAN, "ForTests").setNoRestriction().build());
        attModels.add(new AttributeModelBuilder("PROFILE", PropertyType.STRING, "ForTests").setEnumerationRestriction(
            "public",
            "scientist",
            "user").build());
        attModels.add(new AttributeModelBuilder("DATA", PropertyType.DOUBLE_ARRAY, "ForTests").setDescription(
            "physical data").setNoRestriction().build());

        Mockito.when(mockFragmentRepository.findById(fragmentId)).thenReturn(Optional.of(expected));
        Mockito.when(mockAttModelRepository.findByFragmentId(fragmentId)).thenReturn(attModels);

        try {
            OutputStream output = Files.newOutputStream(Paths.get("target", expected.getName() + ".xml"));
            fragmentSrv.exportFragment(fragmentId, output);
        } catch (IOException e) {
            LOGGER.debug("Cannot export fragment");
            Assert.fail();
        }
    }

    /**
     * Import fragment (See sample-fragment.xml)
     *
     * @throws ModuleException if error occurs!
     */
    @Test
    public void importFragmentTest() throws ModuleException {
        try {
            InputStream input = Files.newInputStream(Paths.get("src", "test", "resources", "sample-fragment.xml"));

            fragmentSrv.importFragment(input);

            // Capture read data
            Mockito.verify(mockAttModelSrv).addAllAttributes(attModelCaptor.capture());
            Iterable<AttributeModel> attModels = attModelCaptor.getValue();

            int expectedSize = 3;
            Assert.assertEquals(expectedSize, Iterables.size(attModels));

            for (AttributeModel attModel : attModels) {
                // Check fragment
                Assert.assertEquals("fragmentName", attModel.getFragment().getName());
                Assert.assertEquals("forTests", attModel.getLabel());
                String name = attModel.getName();

                if ("NAME".equals(name)) {
                    Assert.assertNull(attModel.getDescription());
                    Assert.assertEquals(PropertyType.BOOLEAN, attModel.getType());
                    Assert.assertFalse(attModel.isAlterable());
                    Assert.assertTrue(attModel.isOptional());
                    Assert.assertNull(attModel.getRestriction());
                } else if ("PROFILE".equals(name)) {
                    Assert.assertNull(attModel.getDescription());
                    Assert.assertEquals(PropertyType.STRING, attModel.getType());
                    Assert.assertFalse(attModel.isAlterable());
                    Assert.assertFalse(attModel.isOptional());
                    Assert.assertNotNull(attModel.getRestriction());
                    Assert.assertTrue(attModel.getRestriction() instanceof EnumerationRestriction);
                    EnumerationRestriction er = (EnumerationRestriction) attModel.getRestriction();
                    Assert.assertTrue(er.getAcceptableValues().contains("public"));
                    Assert.assertTrue(er.getAcceptableValues().contains("scientist"));
                    Assert.assertTrue(er.getAcceptableValues().contains("user"));
                } else if ("DATA".equals(name)) {
                    Assert.assertEquals("physical data", attModel.getDescription());
                    Assert.assertEquals(PropertyType.DOUBLE_ARRAY, attModel.getType());
                    Assert.assertTrue(attModel.isAlterable());
                    Assert.assertFalse(attModel.isOptional());
                    Assert.assertNull(attModel.getRestriction());
                } else {
                    Assert.fail("Unexpected attribute");
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Cannot import fragment");
            Assert.fail();
        }
    }

    @Test
    public void test_getFragments() {
        // Given
        List<Fragment> fragments = new ArrayList<>();
        fragments.add(Fragment.buildFragment("bFragment", "description"));
        fragments.add(Fragment.buildFragment("aFragment", "description"));
        fragments.add(Fragment.buildFragment("cFragment", "description"));

        Mockito.when(mockFragmentRepository.findAll()).thenReturn(fragments);

        // When
        List<Fragment> results = fragmentSrv.getFragments();

        // Then
        Assert.assertEquals(3, results.size());
        Assert.assertEquals("aFragment", results.get(0).getName());
        Assert.assertEquals("bFragment", results.get(1).getName());
        Assert.assertEquals("cFragment", results.get(2).getName());
    }
}
