package fr.cnes.regards.framework.staf.event;

public class CollectEventOffLine extends CollectEvent {

    public CollectEventOffLine(Object source) {
        super(source);
    }

    @Override
    public boolean isOnline() {
        return false;
    }

}
