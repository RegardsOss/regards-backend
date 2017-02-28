/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure.multitransactional;

import java.util.List;

/**
 * @author Marc Sordi
 *
 */
public interface ITodoService {

    List<Todo> findAll();

    Todo saveAndPublish(Todo pTodo, boolean pCrash);

    Todo pollAndSave(boolean pCrash);
}