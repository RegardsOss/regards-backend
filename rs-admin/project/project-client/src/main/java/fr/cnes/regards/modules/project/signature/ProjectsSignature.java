/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.signature;

import java.util.List;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.project.domain.Project;

public interface ProjectsSignature {

    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    @ResourceAccess(description = "retrieve the list of project of instance")
    @ResponseBody
    HttpEntity<List<Resource<Project>>> retrieveProjectList();

    @RequestMapping(value = "", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @ResourceAccess(description = "create a new project")
    @ResponseBody
    HttpEntity<Resource<Project>> createProject(@Valid @RequestBody Project newProject) throws AlreadyExistingException;

    @RequestMapping(method = RequestMethod.GET, value = "/{project_id}", produces = "application/json")
    @ResourceAccess(description = "retrieve the project project_id")
    @ResponseBody
    HttpEntity<Resource<Project>> retrieveProject(@PathVariable("project_id") String projectId);

    @RequestMapping(method = RequestMethod.PUT, value = "/{project_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "update the project project_id")
    @ResponseBody
    HttpEntity<Void> modifyProject(@PathVariable("project_id") String projectId, @RequestBody Project projectUpdated)
            throws OperationNotSupportedException;

    @RequestMapping(method = RequestMethod.DELETE, value = "/{project_id}", produces = "application/json")
    @ResourceAccess(description = "remove the project project_id")
    @ResponseBody
    HttpEntity<Void> deleteProject(@PathVariable("project_id") String projectId);
}
