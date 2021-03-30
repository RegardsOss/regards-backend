/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.toponyms.service;

import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.toponyms.dao.IToponymsRepository;
import fr.cnes.regards.modules.toponyms.domain.Toponym;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymLocaleEnum;
import fr.cnes.regards.modules.toponyms.domain.ToponymMetadata;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/**
 *
 * @author Sébastien Binda
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=toponyms_service_it"})
public class ToponymsServiceIT extends AbstractMultitenantServiceTest {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ToponymsService service;

    @Autowired
    private IToponymsRepository toponymRepo;

    private final String LOCALE = ToponymLocaleEnum.EN.getLocale();

    private final boolean TOPONYM_VISIBILITY = true;

    private List<Toponym> temporaryToponyms;

    @Before
    public void init() {
        tenantResolver.forceTenant(getDefaultTenant());
        // delete all temporary toponyms and init new ones
        this.toponymRepo.deleteByVisible(false);
        this.temporaryToponyms = initNotVisibleToponyms();
    }

    @Test
    public void findAllByVisibility() throws IOException, ModuleException, URISyntaxException {
        Page<ToponymDTO> results = service.findAllByVisibility(LOCALE, TOPONYM_VISIBILITY, PageRequest.of(0, 100));
        Assert.assertEquals(251, results.getTotalElements());
        Assert.assertEquals(100, results.getSize());
    }

    @Test
    public void search() {
        List<ToponymDTO> toponyms = service.search("Fran", "en", TOPONYM_VISIBILITY, 100);
        Assert.assertEquals(1, toponyms.size());

        toponyms = service.search("fr", "en", TOPONYM_VISIBILITY, 100);
        Assert.assertEquals(6, toponyms.size());

        toponyms = service.search("e", "en", TOPONYM_VISIBILITY, 100);
        Assert.assertEquals(100, toponyms.size());
    }

    @Test
    public void searchSimplifiedGeo() {
        Optional<ToponymDTO> result = service.findOne("France", true);
        int size = ((MultiPolygon) result.get().getGeometry()).getCoordinates().stream()
                .map(c -> c.stream().map(p -> p.size()).reduce(0, Integer::sum)).reduce(0, Integer::sum);
        Assert.assertEquals("With simplify algorithm france should contains 141 positions", 141, size);

        result = service.findOne("France", false);
        size = ((MultiPolygon) result.get().getGeometry()).getCoordinates().stream()
                .map(c -> c.stream().map(p -> p.size()).reduce(0, Integer::sum)).reduce(0, Integer::sum);
        Assert.assertEquals("Without simplify algorithm france should contains 1042 positions", 1042, size);
    }

    @Test
    @Purpose("Check if expirationDate is updated only for not visible toponyms")
    public void findToponym() {
        Optional<ToponymDTO> visibleToponym = service.findOne("France", false);
        Assert.assertTrue(String.format("Toponym %s should be present", visibleToponym), visibleToponym.isPresent());
        Assert.assertTrue("expirationDate of a visible toponym should always be empty", visibleToponym.get().getToponymMetadata().getExpirationDate() == null);

        Optional<ToponymDTO> notVisibleToponym = service.findOne(this.temporaryToponyms.get(0).getBusinessId(), false);
        Assert.assertTrue(String.format("Toponym %s should be present", notVisibleToponym), notVisibleToponym.isPresent());
        Assert.assertTrue("expirationDate should have been updated", notVisibleToponym.get().getToponymMetadata().getExpirationDate() != null);
    }

    private List<Toponym> initNotVisibleToponyms() {
        List<Toponym> notVisibleToponyms = new ArrayList<>();
        int nbToponyms = 10;
        for (int i = 0; i < nbToponyms; i++) {
            String name = "ToponymTest " + i;
            notVisibleToponyms.add(new Toponym(name, name, name, null, null, null, false, new ToponymMetadata()));
        }
        return this.toponymRepo.saveAll(notVisibleToponyms);
    }
}
