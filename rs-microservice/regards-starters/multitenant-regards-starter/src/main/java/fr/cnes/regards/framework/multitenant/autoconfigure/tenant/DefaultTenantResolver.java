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
package fr.cnes.regards.framework.multitenant.autoconfigure.tenant;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Value;

import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * Resolve tenant base on configuration properties
 * @author msordi
 */
public class DefaultTenantResolver implements ITenantResolver {

    /**
     * List of configurated tenants
     */
    @Value("${regards.tenants:#{null}}")
    private String[] localTenants;

    /**
     * List of unique tenants
     */
    private Set<String> tenants;

    @PostConstruct
    public void init() {
        tenants = new TreeSet<>();
        if (localTenants != null) {
            Collections.addAll(tenants, localTenants);
        }
    }

    @Override
    public Set<String> getAllTenants() {
        return tenants;
    }

    @Override
    public Set<String> getAllActiveTenants() {
        return getAllTenants();
    }

}
