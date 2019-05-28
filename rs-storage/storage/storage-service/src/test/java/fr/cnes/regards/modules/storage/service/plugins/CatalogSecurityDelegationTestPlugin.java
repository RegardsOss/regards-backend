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
package fr.cnes.regards.modules.storage.service.plugins;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;

/**
 * Default {@link ISecurityDelegation} implementation using rs-catalog to check access rights
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team", description = "Plugin handling the security thanks to catalog",
        id = "CatalogSecurityDelegationTest", version = "1.0", contact = "regards@c-s.fr", license = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class CatalogSecurityDelegationTestPlugin implements ISecurityDelegation {

    @Override
    public Set<UniformResourceName> hasAccess(Collection<UniformResourceName> urns) {
        return Sets.newHashSet(urns);
    }

    @Override
    public boolean hasAccess(String ipId) {
        return true;
    }

    @Override
    public boolean hasAccessToListFeature() {
        return true;
    }
}
