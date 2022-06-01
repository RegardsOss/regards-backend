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
package fr.cnes.regards.modules.acquisition.service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author sbinda
 */
@Component
@Profile("!nomonitoring")
public class SessionNotificationHandler
    implements IHandler<StepPropertyUpdateRequestEvent>, ApplicationListener<ApplicationReadyEvent> {

    List<StepPropertyUpdateRequestEvent> events = Lists.newArrayList();

    @Autowired
    private ISubscriber subscriber;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        // Subscribe to events on {@link StorageDataFile} changes.
        subscriber.subscribeTo(StepPropertyUpdateRequestEvent.class, this);
    }

    @Override
    public void handle(TenantWrapper<StepPropertyUpdateRequestEvent> wrapper) {
        events.add(wrapper.getContent());
        this.log(wrapper.getContent());
    }

    private void log(StepPropertyUpdateRequestEvent event) {
        StepPropertyInfo eventInfo = event.getStepProperty().getStepPropertyInfo();
        LOGGER.info("[{}] {}.{}={} {}",
                    eventInfo.getState(),
                    event.getStepProperty().getStepId(),
                    event.getStepProperty(),
                    event.getType(),
                    eventInfo.getValue());
    }

    public long getPropertyCount(String source,
                                 String session,
                                 String step,
                                 String property,
                                 StepPropertyEventTypeEnum type) {
        return events.stream()
                     .filter(e -> e.getStepProperty().getSource().equals(source)
                                  && e.getStepProperty()
                                      .getSession()
                                      .equals(session)
                                  && e.getStepProperty().getStepId().equals(step)
                                  && e.getStepProperty().getStepPropertyInfo().getProperty().equals(property)
                                  && e.getType() == type)
                     .map(event -> Long.parseLong(event.getStepProperty().getStepPropertyInfo().getValue()))
                     .reduce(0L, Long::sum);
    }

    public void clear() {
        events.clear();
    }

}
