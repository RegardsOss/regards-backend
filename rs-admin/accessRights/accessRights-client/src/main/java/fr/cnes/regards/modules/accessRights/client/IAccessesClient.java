package fr.cnes.regards.modules.accessRights.client;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import fr.cnes.regards.client.core.ClientFactory;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

//@FeignClient("accesses")
@Headers("Accept: application/json")
public interface IAccessesClient {

    static IAccessesClient getClient(String pUrl, String pJwtToken) {
        return ClientFactory.build(IAccessesClient.class, new AccessesFallback(), pUrl, pJwtToken);
    }

    @RequestLine("GET /accesses")
    HttpEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList();

    @RequestLine("POST /accesses")
    @Headers("Content-Type: application/json")
    HttpEntity<Resource<ProjectUser>> requestAccess(ProjectUser pAccessRequest) throws AlreadyExistingException;

    @RequestLine("PUT /accesses/{access_id}/accept")
    HttpEntity<Void> acceptAccessRequest(@Param("access_id") Long pAccessId) throws OperationNotSupportedException;

    @RequestLine("PUT /accesses/{access_id}/deny")
    HttpEntity<Void> denyAccessRequest(@Param("access_id") Long pAccessId) throws OperationNotSupportedException;

    @RequestLine("DELETE /accesses/{access_id}")
    HttpEntity<Void> removeAccessRequest(@Param("access_id") Long pAccessId);

    @RequestLine("GET /accesses/settings")
    HttpEntity<List<Resource<String>>> getAccessSettingList();

    @RequestLine("PUT /accesses/settings")
    @Headers("Content-Type: application/json")
    HttpEntity<Void> updateAccessSetting(String pUpdatedProjectUserSetting) throws InvalidValueException;

}
