/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.service.account.AccountWorkflowManager;
import fr.cnes.regards.modules.accessrights.service.projectuser.IAccessSettingsService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.ProjectUserWorkflowManager;
import fr.cnes.regards.modules.accessrights.signature.IAccessesSignature;

@RestController
@ModuleInfo(name = "accessrights", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
public class AccessesController implements IAccessesSignature {

    /**
     * Service handling CRUD operation on access requests. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IProjectUserService projectUserService;

    /**
     * Workflow manager of project users. Autowired by Spring. Must not be <code>null</code>.
     */
    @Autowired
    private ProjectUserWorkflowManager projectUserWorkflowManager;

    /**
     * Workflow manager of account. Autowired by Spring. Must not be <code>null</code>.
     */
    @Autowired
    private AccountWorkflowManager accountWorkflowManager;

    /**
     * Service handling CRUD operation on {@link AccountSettings}. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IAccessSettingsService accessSettingsService;

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccessesSignature#retrieveAccessRequestList()
     */
    @Override
    @ResourceAccess(description = "Retrieves the list of access request")
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList() {
        final List<ProjectUser> projectUsers = projectUserService.retrieveAccessRequestList();
        return new ResponseEntity<>(HateoasUtils.wrapList(projectUsers), HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccessesSignature#requestAccess(fr.cnes.regards.modules.
     * accessrights.domain.AccessRequestDTO)
     */
    @Override
    @ResourceAccess(description = "Creates a new access request")
    public ResponseEntity<Resource<AccessRequestDTO>> requestAccess(
            @Valid @RequestBody final AccessRequestDTO pAccessRequest) throws EntityException {
        accountWorkflowManager.requestAccount(pAccessRequest);
        projectUserWorkflowManager.requestProjectAccess(pAccessRequest);
        final Resource<AccessRequestDTO> resource = new Resource<>(pAccessRequest);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccessesSignature#acceptAccessRequest(java.lang.Long)
     */
    @Override
    @ResourceAccess(description = "Accepts the access request")
    public ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws EntityTransitionForbiddenException, EntityNotFoundException {
        final ProjectUser projectUser = projectUserService.retrieveUser(pAccessId);
        projectUserWorkflowManager.grantAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccessesSignature#denyAccessRequest(java.lang.Long)
     */
    @Override
    @ResourceAccess(description = "Denies the access request")
    public ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws EntityTransitionForbiddenException, EntityNotFoundException {
        final ProjectUser projectUser = projectUserService.retrieveUser(pAccessId);
        projectUserWorkflowManager.denyAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccessesSignature#removeAccessRequest(java.lang.Long)
     */
    @Override
    @ResourceAccess(description = "Rejects the access request")
    public ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(pAccessId);
        projectUserWorkflowManager.removeAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccessesSignature#getAccessSettings()
     */
    @Override
    @ResourceAccess(description = "Retrieves the settings managing the access requests")
    public ResponseEntity<Resource<AccessSettings>> getAccessSettings() {
        final AccessSettings accessSettings = accessSettingsService.retrieve();
        final Resource<AccessSettings> resource = new Resource<>(accessSettings);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.signature.IAccessesSignature#updateAccessSettings(fr.cnes.regards.modules.
     * accessrights.domain.projects.AccessSettings)
     */
    @Override
    @ResourceAccess(description = "Updates the setting managing the access requests")
    public ResponseEntity<Void> updateAccessSettings(@Valid @RequestBody final AccessSettings pAccessSettings)
            throws EntityNotFoundException {
        accessSettingsService.update(pAccessSettings);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
