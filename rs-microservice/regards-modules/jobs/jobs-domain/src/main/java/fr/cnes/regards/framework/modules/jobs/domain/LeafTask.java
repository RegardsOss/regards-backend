package fr.cnes.regards.framework.modules.jobs.domain;

import java.util.Collections;
import java.util.Set;

/**
 * A reliant task that is not reliant.
 * Database mapping is the same as AbstractReliantTask one (to permit creating trees of dependent tasks) but as this
 * task depends on nothing, to avoid lazy exceptions while retrieving reliant tasks, accessor method are bypassed.
 * @author oroussel
 */
@SuppressWarnings("rawtypes")
public class LeafTask extends AbstractReliantTask<AbstractReliantTask> {

    @Override
    public Set<AbstractReliantTask> getReliantTasks() {
        return Collections.emptySet();
    }

    @Override
    public void addReliantTask(AbstractReliantTask reliantTask) {
        throw new IllegalAccessError("A leaf task cannot be reliant on another task");
    }
}
