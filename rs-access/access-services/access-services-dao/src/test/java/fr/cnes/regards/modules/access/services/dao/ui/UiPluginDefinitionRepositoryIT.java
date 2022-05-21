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
package fr.cnes.regards.modules.access.services.dao.ui;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.net.MalformedURLException;

/**
 * Unit test for {@link IUIPluginDefinitionRepository}
 *
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource("classpath:test.properties")
public class UiPluginDefinitionRepositoryIT extends AbstractDaoTransactionalIT {

    @Autowired
    private IUIPluginDefinitionRepository repository;

    @Test
    public void save() throws MalformedURLException {
        UIPluginDefinition uiPluginDefinition = UIPluginDefinition.build("My Cool Plugin",
                                                                         "the/source/path",
                                                                         UIPluginTypesEnum.SERVICE);
        uiPluginDefinition.setId(0L);
        uiPluginDefinition.setIconUrl("http://wwww.google.com");
        uiPluginDefinition.setEntityTypes(Sets.newHashSet(EntityType.COLLECTION, EntityType.DATA));
        uiPluginDefinition.setApplicationModes(Sets.newHashSet(ServiceScope.ONE, ServiceScope.MANY));
        repository.save(uiPluginDefinition);
    }

}
