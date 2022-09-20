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
package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.HashSet;

/**
 * Event to submit a request to cancel all requests associated to given group ids
 *
 * @author Sébastien Binda
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class CancelRequestEvent implements ISubscribable {

    @NotNull @NotEmpty Collection<String> groupsToCancel = new HashSet<>();

    public CancelRequestEvent(Collection<String> groupsToCancel) {
        this.groupsToCancel.addAll(groupsToCancel);
    }

    public CancelRequestEvent() {
    }

    public Collection<String> getGroupsToCancel() {
        return groupsToCancel;
    }

    public void setGroupsToCancel(Collection<String> groupsToCancel) {
        this.groupsToCancel.clear();
        this.groupsToCancel.addAll(groupsToCancel);
    }
}
