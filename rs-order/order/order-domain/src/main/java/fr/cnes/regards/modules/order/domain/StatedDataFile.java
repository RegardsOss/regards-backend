package fr.cnes.regards.modules.order.domain;

import java.util.Comparator;

import fr.cnes.regards.modules.indexer.domain.DataFile;

/**
 * Inherits from DataFile to add nearline state
 * @author oroussel
 */
public class StatedDataFile extends DataFile {

    private FileState state;

    public FileState getState() {
        return state;
    }

    public void setState(FileState state) {
        this.state = state;
    }

    @Override
    public void setOnline(Boolean online) {
        super.setOnline(online);
        if (online) {
            this.state = FileState.ONLINE;
        }
    }
}
