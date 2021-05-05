package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ISessionDeleteService} if no bean is provided
 *
 * @author Iliana Ghazali
 **/
public class DefaultSessionDeleteService implements ISessionDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSessionDeleteService.class);

    @Override
    public void deleteSession(String source, String session) {
        LOGGER.warn("Bean missing to delete session, the session {} from source {} will not be deleted", session,
                    source);
    }
}
