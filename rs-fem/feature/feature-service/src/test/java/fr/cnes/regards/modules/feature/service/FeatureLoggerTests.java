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
package fr.cnes.regards.modules.feature.service;

import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.feature.dto.urn.FeatureIdentifier;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.logger.FeatureLogger;

public class FeatureLoggerTests {

    private static final String REQUEST_OWNER = "owner";

    private static final String REQUEST_ID = UUID.randomUUID().toString();

    private static final String PROVIDER_ID = "provider";

    private static final FeatureUniformResourceName URN = FeatureUniformResourceName
            .pseudoRandomUrn(FeatureIdentifier.FEATURE, EntityType.DATA, "tenant", 1);

    private static final Set<String> REQUEST_ERRORS = Sets.newHashSet("error1", "error2");

    @Test
    public void creationDenied() {
        FeatureLogger.creationDenied(REQUEST_OWNER, REQUEST_ID, PROVIDER_ID, REQUEST_ERRORS);
    }

    @Test
    public void creationGranted() {
        FeatureLogger.creationGranted(REQUEST_OWNER, REQUEST_ID, PROVIDER_ID);
    }

    @Test
    public void creationSuccess() {
        FeatureLogger.creationSuccess(REQUEST_OWNER, REQUEST_ID, PROVIDER_ID, URN);
    }

    @Test
    public void updateDenied() {
        FeatureLogger.updateDenied(REQUEST_OWNER, REQUEST_ID, PROVIDER_ID, URN, REQUEST_ERRORS);
    }

    @Test
    public void updateGranted() {
        FeatureLogger.updateGranted(REQUEST_OWNER, REQUEST_ID, PROVIDER_ID, URN);
    }

    @Test
    public void updateSuccess() {
        FeatureLogger.updateSuccess(REQUEST_OWNER, REQUEST_ID, PROVIDER_ID, URN);
    }

    @Test
    public void updateError() {
        FeatureLogger.updateError(REQUEST_OWNER, REQUEST_ID, PROVIDER_ID, URN, REQUEST_ERRORS);
    }

    @Test
    public void deletionDenied() {
        FeatureLogger.deletionDenied(REQUEST_OWNER, REQUEST_ID, URN, REQUEST_ERRORS);
    }

    @Test
    public void deletionGranted() {
        FeatureLogger.deletionGranted(REQUEST_OWNER, REQUEST_ID, URN);
    }

    @Test
    public void deletionSuccess() {
        FeatureLogger.deletionSuccess(REQUEST_OWNER, REQUEST_ID, URN);
    }

    @Test
    public void notificationDenied() {
        FeatureLogger.notificationDenied(REQUEST_OWNER, REQUEST_ID, URN, REQUEST_ERRORS);
    }

    @Test
    public void notificationGranted() {
        FeatureLogger.notificationGranted(REQUEST_OWNER, REQUEST_ID, URN);
    }

    @Test
    public void notificationSuccess() {
        FeatureLogger.notificationSuccess(REQUEST_OWNER, REQUEST_ID, URN);
    }
}
