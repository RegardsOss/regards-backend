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
package fr.cnes.regards.modules.indexer.dao.spatial;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.ResponseEntityUtils;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.indexer.domain.spatial.Crs;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static fr.cnes.regards.framework.feign.security.FeignSecurityManager.asSystem;
import static fr.cnes.regards.framework.feign.security.FeignSecurityManager.reset;

/**
 * Projects settings concerning geospatial behavior.<br/>
 *
 * @author oroussel
 */
@Component
public class ProjectGeoSettings {

    @Autowired
    private IProjectsClient projectsClient;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    /**
     * WARNING : The algorithm beneath this property is not 100% valid. This property should always be false. Use it
     * only for tests.
     */
    @Value("${regards.geometry.check.polygon.orientation:false}")
    private boolean checkPolygonOrientation = false;

    /**
     * Using a cache to manage projects values and to be refreshed every 1 minute in case project properties have
     * changed.
     * This cache contains Crs and shouldManagePolesOnGeometries values associated to projects
     */
    private final LoadingCache<String, Pair<Boolean, Crs>> settingsCache = CacheBuilder.newBuilder()
                                                                                       .expireAfterWrite(1,
                                                                                                         TimeUnit.MINUTES)
                                                                                       .build(new CacheLoader<String, Pair<Boolean, Crs>>() {

                                                                                           @Override
                                                                                           public Pair<Boolean, Crs> load(
                                                                                               String key) {
                                                                                               try {
                                                                                                   asSystem();

                                                                                                   ResponseEntity<EntityModel<Project>> response = projectsClient.retrieveProject(
                                                                                                       tenantResolver.getTenant());
                                                                                                   if (response.getStatusCode()
                                                                                                       == HttpStatus.OK) {
                                                                                                       Project currentProject = ResponseEntityUtils.extractContentOrThrow(
                                                                                                           response,
                                                                                                           () -> new RsRuntimeException(
                                                                                                               "Error while retrieving project"));
                                                                                                       // To avoid problems later...No CRS => WGS84
                                                                                                       if (currentProject.getCrs()
                                                                                                           == null) {
                                                                                                           currentProject.setCrs(
                                                                                                               Crs.WGS_84.toString());
                                                                                                       }
                                                                                                       return Pair.of(
                                                                                                           currentProject.getPoleToBeManaged(),
                                                                                                           Crs.valueOf(
                                                                                                               currentProject.getCrs()));
                                                                                                   } else { // Must throw something
                                                                                                       throw new RsRuntimeException(
                                                                                                           new Exception(
                                                                                                               String.format(
                                                                                                                   "Error while asking project client: Error %d - %s",
                                                                                                                   response.getStatusCode()
                                                                                                                           .value(),
                                                                                                                   ((HttpStatus) response.getStatusCode()).getReasonPhrase())));
                                                                                                   }
                                                                                               } finally {
                                                                                                   reset();
                                                                                               }
                                                                                           }
                                                                                       });

    /**
     * @return current tenant/project shouldManagePolesOnGeometries property
     */
    public Boolean getShouldManagePolesOnGeometries() {
        return settingsCache.getUnchecked(tenantResolver.getTenant()).getLeft();
    }

    /**
     * Check polygon orientation in geometry calculation to revert points if not counterclockwise.
     * WARNING : The algorithm beneath this property is not 100% valid. This property should always be false. Use it
     * only for tests.
     */
    public boolean checkPolygonOrientation() {
        return checkPolygonOrientation;
    }

    /**
     * @return current tenant/project crs property
     */
    public Crs getCrs() {
        return settingsCache.getUnchecked(tenantResolver.getTenant()).getRight();
    }
}
