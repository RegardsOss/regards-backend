package fr.cnes.regards.modules.accessrights.service.utils;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.emails.service.IEmailService;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AccessRightsEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessRightsEmailService.class);

    private final ITemplateService templateService;

    private final IEmailService emailService;

    private final IAccountsClient accountsClient;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    public AccessRightsEmailService(ITemplateService templateService,
                                    IEmailService emailService,
                                    IAccountsClient accountsClient,
                                    IRuntimeTenantResolver runtimeTenantResolver) {
        this.templateService = templateService;
        this.emailService = emailService;
        this.accountsClient = accountsClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    public void sendEmail(AccessRightsEmailWrapper wrapper) {

        String message;
        String name = wrapper.getProjectUser().getFirstName();
        String email = wrapper.getProjectUser().getEmail();
        if (StringUtils.isBlank(name)) {
            name = getNameFromAccount(email);
        }
        wrapper.getData().put("name", name);
        wrapper.getData().put("email", email);
        wrapper.getData().put("project", runtimeTenantResolver.getTenant());

        try {
            message = templateService.render(wrapper.getTemplate(), wrapper.getData());
        } catch (final TemplateException e) {
            LOGGER.error("Could not find template to generate the message. Falling back to default.", e);
            message = wrapper.getDefaultMessage();
        }
        emailService.sendEmail(message,
                               wrapper.getSubject(),
                               wrapper.getFrom(),
                               wrapper.getTo().toArray(new String[0]));
    }

    private String getNameFromAccount(String email) {
        String name = null;
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<EntityModel<Account>> accountResponse = accountsClient.retrieveAccounByEmail(email);
            if (accountResponse.getStatusCode().is2xxSuccessful()) {
                EntityModel<Account> body = accountResponse.getBody();
                if (body != null) {
                    Account content = body.getContent();
                    if (content != null) {
                        name = content.getFirstName();
                    }
                }
            }
        } finally {
            FeignSecurityManager.reset();
        }
        return name;
    }

}
