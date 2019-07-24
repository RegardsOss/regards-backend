/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.flow;

import javax.validation.constraints.NotNull;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * Data flow item to store AIP using event driven mechanism.
 *
 * @author Marc SORDI
 *
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class AipFlowItem implements ISubscribable {

    @NotNull
    private AIP aip;

    public void setAip(AIP aip) {
        this.aip = aip;
    }

    public AIP getAip() {
        return aip;
    }

    public static AipFlowItem build(AIP aip) {
        Assert.notNull(aip, "AIP is required");
        AipFlowItem item = new AipFlowItem();
        item.setAip(aip);
        return item;
    }
}
