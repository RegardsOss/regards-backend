package fr.cnes.regards.modules.search.service;

import feign.Response;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.modules.dataaccess.client.IAccessRightClient;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Provides an endpoint that cheks if the current user has access rights to the dataset
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

    public Response getFile(UniformResourceName datasetIpId, HttpServletResponse response) throws EntityOperationForbiddenException, IOException, EntityNotFoundException {
        final String datasetIpIdAsString = datasetIpId.toString();

        // Retrieve current user from security context
        final String userEmail = SecurityUtils.getActualUser();
        Assert.notNull(userEmail, "No user found!");
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<Boolean> isUserAutorisedToAccessDataset = accessRightClient.isUserAutorisedToAccessDataset(datasetIpId, userEmail);
            if (isUserAutorisedToAccessDataset.getBody()) {
                Response fileStream = datasetClient.retrieveDatasetDescription(datasetIpIdAsString);
                return fileStream;
            } else {
                throw new EntityOperationForbiddenException(datasetIpIdAsString, Dataset.class, "You are not allowed to access to the dataset");
            }
        } finally {
            FeignSecurityManager.reset();
        }
    }
}
