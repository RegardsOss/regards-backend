package fr.cnes.regards.framework.modules.session.commons.service.delete;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ISourceDeleteService} if no bean is provided
 *
 * @author Iliana Ghazali
 **/
public class DefaultSourceDeleteService implements ISourceDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSourceDeleteService.class);

    @Override
    public void deleteSource(String source) {
        LOGGER.warn("Bean missing to delete the source {}. It will not be deleted", source);
    }
}