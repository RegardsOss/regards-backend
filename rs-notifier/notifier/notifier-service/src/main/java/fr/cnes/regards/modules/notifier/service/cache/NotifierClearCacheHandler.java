/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.service.cache;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.notifier.dto.internal.NotifierClearCacheEvent;
import fr.cnes.regards.modules.notifier.service.RuleCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler of {@link NotifierClearCacheEvent} events
 *
 * @author Léo Mieulet
 */
@Component
@Profile("!nohandler")
public class NotifierClearCacheHandler
    implements IBatchHandler<NotifierClearCacheEvent>, ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private RuleCache ruleCache;

    @Override
    public Class<NotifierClearCacheEvent> getMType() {
        return NotifierClearCacheEvent.class;
    }

    @Override
    public Errors validate(NotifierClearCacheEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<NotifierClearCacheEvent> messages) {
        ruleCache.clear();
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(NotifierClearCacheEvent.class, this);
    }
}
