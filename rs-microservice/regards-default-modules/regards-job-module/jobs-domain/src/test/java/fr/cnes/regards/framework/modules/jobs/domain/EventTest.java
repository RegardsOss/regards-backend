/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jobs.domain;

import java.util.ArrayList;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import fr.cnes.regards.framework.modules.jobs.domain.Event;
import fr.cnes.regards.framework.modules.jobs.domain.EventType;

/**
 *
 */
public class EventTest {

    @Test
    public void testDomain() {
        final long jobInfoId = 5L;
        final ArrayList<Object> data = new ArrayList<>();
        final EventType eventType = EventType.RUN_ERROR;
        final String tenantName = "project1";
        final Event event1 = new Event(eventType, data, jobInfoId, tenantName);

        Assertions.assertThat(event1.getJobInfoId()).isEqualTo(jobInfoId);
        Assertions.assertThat(event1.getData()).isEqualTo(data);
        Assertions.assertThat(event1.getType()).isEqualTo(eventType);
        EventType.valueOf(EventType.RUN_ERROR.toString());

    }
}
