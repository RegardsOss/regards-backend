/*
 * LICENSE_PLACEHOLDER
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
 *
 * @author Marc Sordi
 *
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
