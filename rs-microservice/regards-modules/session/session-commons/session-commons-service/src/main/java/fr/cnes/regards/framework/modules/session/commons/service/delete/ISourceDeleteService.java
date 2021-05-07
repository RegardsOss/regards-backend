package fr.cnes.regards.framework.modules.session.commons.service.delete;

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
