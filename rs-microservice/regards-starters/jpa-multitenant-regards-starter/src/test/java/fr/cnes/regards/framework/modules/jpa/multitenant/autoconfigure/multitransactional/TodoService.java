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
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.multitransactional;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;

/**
 * Minimal test service to test transaction synchronization between data source and message
 * @author Marc Sordi
 */
@Service
public class TodoService implements ITodoService {

    @Autowired
    private ITodoRepository todoRepository;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IPoller poller;

    @Override
    public List<Todo> findAll() {
        return Lists.newArrayList(todoRepository.findAll());
    }

    @Override
    @RegardsTransactional
    public Todo saveAndPublish(Todo pTodo, boolean pCrash) {
        // Save in database
        Todo todo = todoRepository.save(pTodo);
        TodoEvent event = new TodoEvent();
        event.setLabel(pTodo.getLabel());
        // Publish
        publisher.publish(event);
        // Crash?
        if (pCrash) {
            throw new UnsupportedOperationException("Crash on save and publish!");
        }
        return todo;
    }

    @Override
    @RegardsTransactional
    public Todo pollAndSave(boolean pCrash) {
        // Poll todo
        TenantWrapper<TodoEvent> wrapper = poller.poll(TodoEvent.class);
        // Save in database
        Todo todo = new Todo();
        todo.setLabel(wrapper.getContent().getLabel());
        todo = todoRepository.save(todo);
        // Crash?
        if (pCrash) {
            throw new UnsupportedOperationException("Crash on poll and save!");
        }
        return todo;
    }
}
