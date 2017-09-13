/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.event;

import java.nio.file.Path;
import java.util.EventObject;
import java.util.Set;

/**
 * Event for STAF System file restoration.
 * @author Sébastien Binda
 */
@SuppressWarnings("serial")
public class CollectEvent extends EventObject {

    /**
     * Files successfully restored.
     */
    private Set<Path> restoredFilePaths;

    /**
     * Files not restored.
     */
    private Set<Path> notRestoredFilePaths;

    /**
     * Constructor
     * @param source
     */
    public CollectEvent(Object source) {
        super(source);
    }

    public Set<Path> getRestoredFilePaths() {
        return restoredFilePaths;
    }

    public void setRestoredFilePaths(Set<Path> pRestoredFilePaths) {
        restoredFilePaths = pRestoredFilePaths;
    }

    public Set<Path> getNotRestoredFilePaths() {
        return notRestoredFilePaths;
    }

    public void setNotRestoredFilePaths(Set<Path> pNotRestoredFilePaths) {
        notRestoredFilePaths = pNotRestoredFilePaths;
    }

}
