package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 *
 * @author Iliana Ghazali
 **/
public abstract class AbstractSessionHandler<T extends ISubscribable> implements IBatchHandler<T> {


    private final Class<T> type;


    public AbstractSessionHandler(Class<T> type) {
        this.type = type;
    }

    @Override
    public Class<T> getMType() {
        return type;
    }

    /**
     * Abstract method to delete all elements of a session
     * @param session name of the session to delete
     * @return if session is successfully deleted
     */
    protected abstract boolean deleteSession(String session);

    /**
     * Abstract method to delete all elements of a source
     * @param source name of the source to delete
     * @return if source is successfully deleted
     */
    protected abstract boolean deleteSource(String source);
}
