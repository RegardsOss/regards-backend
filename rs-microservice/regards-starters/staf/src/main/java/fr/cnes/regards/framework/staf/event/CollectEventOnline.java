package fr.cnes.regards.framework.staf.event;

public class CollectEventOnline extends CollectEvent {

    public CollectEventOnline(Object source) {
        super(source);
    }

    @Override
    public boolean isOnline() {
        return true;
    }

}
