/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.multitransactional;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.IPoller;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * Test multi transaction synchronization with AMQP and database transactions.
 *
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { MultiTransactionTestConfiguration.class })
public class MultiTransactionTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTransactionTest.class);

    /**
     * Todo service
     */
    @Autowired
    private ITodoService todoService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IPoller poller;

    @Before
    public void init() {
        tenantResolver.forceTenant("test1");
    }

    @Test
    public void transactionalPublishCrashTest() {

        Todo todo = genTodo();

        // Try to save and publish event with error
        try {
            todoService.saveAndPublish(todo, true);
        } catch (UnsupportedOperationException e) {
            // Rollback must occurs for both database and broker
            LOGGER.debug("Expected exception occurs : {}", e.getMessage());
        }

        // Test whether no message was published
        TenantWrapper<TodoEvent> wrapper = poller.poll(TodoEvent.class);
        Assert.assertNull(wrapper);

        // Test nothing was saved on database
        List<Todo> todos = todoService.findAll();
        Assert.assertTrue(!todos.contains(todo));
    }

    @Test
    public void operationalPublishTest() {

        Todo todo = genTodo();

        // Save and publish event without error
        Todo saved = todoService.saveAndPublish(todo, false);
        Assert.assertNotNull(saved.getId());

        // Test whether a message was published on the broker
        TenantWrapper<TodoEvent> wrapper = poller.poll(TodoEvent.class);
        Assert.assertNotNull(wrapper);
        Assert.assertEquals(todo.getLabel(), wrapper.getContent().getLabel());

        // Test todo was saved on database
        List<Todo> todos = todoService.findAll();
        Assert.assertTrue(todos.contains(todo));
    }

    @Test
    public void transactionalPollCrashTest() {

        Todo todo = genTodo();
        TodoEvent event = new TodoEvent();
        event.setLabel(todo.getLabel());
        // Publish event
        publisher.publish(event);

        // Try to poll and save with error
        try {
            todoService.pollAndSave(true);
        } catch (UnsupportedOperationException e) {
            // Rollback must occurs for both database and broker
            LOGGER.debug("Expected exception occurs : {}", e.getMessage());
        }

        // Test whether message already ready to be polled on the broker
        TenantWrapper<TodoEvent> wrapper = poller.poll(TodoEvent.class);
        Assert.assertNotNull(wrapper);

        // Test nothing was saved on database
        List<Todo> todos = todoService.findAll();
        Assert.assertTrue(!todos.contains(todo));
    }

    @Test
    public void operationalPollTest() {

        Todo todo = genTodo();
        TodoEvent event = new TodoEvent();
        event.setLabel(todo.getLabel());
        // Publish event
        publisher.publish(event);

        // Poll and save without error
        todoService.pollAndSave(false);

        // Test whether message was consumed on the broker
        TenantWrapper<TodoEvent> wrapper = poller.poll(TodoEvent.class);
        Assert.assertNull(wrapper);

        // Test todo was saved on database
        List<Todo> todos = todoService.findAll();
        Assert.assertTrue(todos.contains(todo));
    }

    /**
     * Generate a {@link Todo} with a random unique label
     *
     * @return random {@link Todo}
     */
    private Todo genTodo() {
        Todo todo = new Todo();
        String label = UUID.randomUUID().toString();
        todo.setLabel(label);
        Assert.assertNull(todo.getId());
        return todo;
    }
}
