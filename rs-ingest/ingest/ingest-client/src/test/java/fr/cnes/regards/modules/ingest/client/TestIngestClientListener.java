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
package fr.cnes.regards.modules.ingest.client;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * Test listener
 *
 * @author Marc SORDI
 */
@Component
public class TestIngestClientListener implements IIngestClientListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestIngestClientListener.class);

    private final Set<RequestInfo> denied = Sets.newHashSet();

    private final Set<RequestInfo> granted = Sets.newHashSet();

    private final Set<RequestInfo> success = Sets.newHashSet();

    private final Set<RequestInfo> deleted = Sets.newHashSet();

    private final Set<RequestInfo> errors = Sets.newHashSet();

    public void clear() {
        denied.clear();
        success.clear();
        errors.clear();
        granted.clear();
    }

    @Override
    public void onDenied(Collection<RequestInfo> infos) {
        infos.forEach(info -> LOGGER.debug("DENIED ------------- {}", info.getRequestId()));
        denied.addAll(infos);
    }

    @Override
    public void onGranted(Collection<RequestInfo> infos) {
        if (infos.isEmpty()) {
            LOGGER.error("empty !!!!!");
        } else {
            infos.forEach(info -> LOGGER.debug("GRANTED ------------- {}", info.getRequestId()));
        }
        granted.addAll(infos);
    }

    @Override
    public void onError(Collection<RequestInfo> infos) {
        infos.forEach(info -> LOGGER.debug("ERROR ------------- {}", info.getRequestId()));
        errors.addAll(infos);
    }

    @Override
    public void onSuccess(Collection<RequestInfo> infos) {
        infos.forEach(info -> LOGGER.debug("SUCCEED ------------- {}", info.getRequestId(), info.getSipId()));
        success.addAll(infos);
    }

    @Override
    public void onDeleted(Set<RequestInfo> infos) {
        infos.forEach(info -> LOGGER.debug("DELETED ------------- {}", info.getRequestId(), info.getSipId()));
        deleted.addAll(infos);
    }

    /**
     * @return the denied
     */
    public Set<RequestInfo> getDenied() {
        return denied;
    }

    /**
     * @return the granted
     */
    public Set<RequestInfo> getGranted() {
        return granted;
    }

    /**
     * @return the success
     */
    public Set<RequestInfo> getSuccess() {
        return success;
    }

    /**
     * @return the errors
     */
    public Set<RequestInfo> getErrors() {
        return errors;
    }

}
