package fr.cnes.regards.framework.staf.event;

import java.util.EventObject;
import java.util.List;

public class CollectEvent extends EventObject {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 5.3
     */
    private static final long serialVersionUID = 1L;

    List<String> files_ = null;

    Boolean isStopEvent = Boolean.FALSE;

    public CollectEvent(Object source) {
        super(source);
    }

    // V44-FA-VR-FC-SSALTO-ARCH-010-01 : commande de fichiers decoupes
    public boolean isOnline() {
        return true;
    }

    public List<String> getFiles() {
        return files_;
    }

    public void setFiles(List<String> files_) {
        this.files_ = files_;
    }

    public Boolean isStopEvent() {
        return isStopEvent;
    }

    public void setIsStopEvent(Boolean isStopEvent) {
        this.isStopEvent = isStopEvent;
    }

}
