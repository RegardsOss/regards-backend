/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.cache;

import java.util.Set;

import org.assertj.core.util.Sets;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.event.DataFileEventState;

public class TestDataStorageEventHandler implements IHandler<DataFileEvent> {

    private final Set<String> restoredChecksum = Sets.newHashSet();

    @Override
    public synchronized void handle(TenantWrapper<DataFileEvent> pWrapper) {
        if (pWrapper.getContent().getState().equals(DataFileEventState.AVAILABLE)) {
            restoredChecksum.add(pWrapper.getContent().getChecksum());
        }
    }

    public synchronized Set<String> getRestoredChecksum() {
        return restoredChecksum;
    }

    public synchronized void reset() {
        restoredChecksum.clear();
    }

}
