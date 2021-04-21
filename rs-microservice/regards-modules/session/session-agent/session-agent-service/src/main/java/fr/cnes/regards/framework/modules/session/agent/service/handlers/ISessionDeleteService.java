package fr.cnes.regards.framework.modules.session.agent.service.handlers;

/**
 * Interface to delete a session
 * @author Iliana Ghazali
 **/
public interface ISessionDeleteService {

    /**
     * Delete a session
     *
     * @param source  source including the session to delete
     * @param session the session to be deleted
     * @return if session is successfully deleted
     */
    boolean deleteSession(String source, String session);

}
