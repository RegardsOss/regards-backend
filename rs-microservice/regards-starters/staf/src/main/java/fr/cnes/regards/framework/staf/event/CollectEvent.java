package fr.cnes.regards.framework.staf.event;

import java.nio.file.Path;
import java.util.EventObject;
import java.util.Set;

@SuppressWarnings("serial")
public class CollectEvent extends EventObject {

    private Set<Path> restoredFilePaths;

    public CollectEvent(Object source) {
        super(source);
    }

    public Set<Path> getRestoredFilePaths() {
        return restoredFilePaths;
    }

    public void setRestoredFilePaths(Set<Path> pRestoredFilePaths) {
        restoredFilePaths = pRestoredFilePaths;
    }

}
