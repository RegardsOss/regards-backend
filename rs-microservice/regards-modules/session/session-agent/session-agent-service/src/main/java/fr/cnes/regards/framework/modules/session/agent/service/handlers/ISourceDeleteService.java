package fr.cnes.regards.framework.modules.session.agent.service.handlers;

/**
 * Interface to delete a source
 *
 * @author Iliana Ghazali
 **/
public interface ISourceDeleteService {

    /**
     * Delete a source
     *
     * @param source source to be deleted
     * @return if source is successfully deleted
     */
    void deleteSource(String source);
}
