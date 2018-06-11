package fr.cnes.regards.modules.search.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import feign.Response;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dataaccess.client.IAccessRightClient;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 * Provides an endpoint that checks if the current user has access rights to the dataset (or is admin)
 * Before retrieve it and send it to user
 *
 * @author Léo Mieulet
 */
@Service
public class FileEntityDescriptionHelper implements IFileEntityDescriptionHelper {

    @Autowired
    private IAccessRightClient accessRightClient;

    @Autowired
    private IDatasetClient datasetClient;

    @Autowired
    private IProjectUsersClient projectUserClient;

    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * Return a file in a response
     */
    public Response getFile(UniformResourceName datasetIpId)
            throws EntityOperationForbiddenException, IOException, EntityNotFoundException {
        final String datasetIpIdAsString = datasetIpId.toString();
        // Retrieve current user from security context
        final String userEmail = authResolver.getUser();
        Assert.notNull(userEmail, "No user found!");
        try {
            FeignSecurityManager.asSystem();
            if (isUserAutorisedToAccessDatasetFile(userEmail, datasetIpId)) {
                Response fileStream = datasetClient.retrieveDatasetDescription(datasetIpIdAsString);
                return fileStream;
            } else {
                throw new EntityOperationForbiddenException(datasetIpIdAsString, Dataset.class,
                        "You are not allowed to access to the dataset");
            }
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * Return true if the user is admin or has accessRight set up on rs-dam
     * @param userEmail
     * @param datasetIpId
     * @return
     */
    private boolean isUserAutorisedToAccessDatasetFile(String userEmail, UniformResourceName datasetIpId) {
        return projectUserClient.isAdmin(userEmail).getBody()
                || accessRightClient.isUserAutorisedToAccessDataset(datasetIpId, userEmail).getBody();
    }
}
