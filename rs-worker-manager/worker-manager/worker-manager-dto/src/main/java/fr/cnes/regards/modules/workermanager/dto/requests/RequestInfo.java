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
package fr.cnes.regards.modules.workermanager.dto.requests;

import com.google.common.collect.Lists;
import fr.cnes.regards.modules.workermanager.dto.events.in.RequestEvent;
import org.springframework.amqp.core.Message;

import java.util.List;

/**
 * Information about handled requests {@link RequestDTO}s
 *
 * @author Sébastien Binda
 */
public class RequestInfo {

    List<RequestDTO> delayedRequests = Lists.newArrayList();

    List<RequestDTO> dispatchedRequests = Lists.newArrayList();

    List<Message> skippedEvents = Lists.newArrayList();

    List<RequestDTO> successRequests = Lists.newArrayList();

    List<RequestDTO> errorRequests = Lists.newArrayList();

    List<RequestDTO> runningRequests = Lists.newArrayList();

    public List<RequestDTO> getDelayedRequests() {
        return delayedRequests;
    }

    public void setDelayedRequests(List<RequestDTO> delayedRequests) {
        this.delayedRequests = delayedRequests;
    }

    public List<RequestDTO> getDispatchedRequests() {
        return dispatchedRequests;
    }

    public void setDispatchedRequests(List<RequestDTO> dispatchedRequests) {
        this.dispatchedRequests = dispatchedRequests;
    }

    public List<Message> getSkippedEvents() {
        return skippedEvents;
    }

    public void setSkippedEvents(List<Message> skippedEvents) {
        this.skippedEvents = skippedEvents;
    }

    public List<RequestDTO> getSuccessRequests() {
        return successRequests;
    }

    public void setSuccessRequests(List<RequestDTO> successRequests) {
        this.successRequests = successRequests;
    }

    public List<RequestDTO> getErrorRequests() {
        return errorRequests;
    }

    public void setErrorRequests(List<RequestDTO> errorRequests) {
        this.errorRequests = errorRequests;
    }

    public List<RequestDTO> getRunningRequests() {
        return runningRequests;
    }

    public void setRunningRequests(List<RequestDTO> runningRequests) {
        this.runningRequests = runningRequests;
    }
}
