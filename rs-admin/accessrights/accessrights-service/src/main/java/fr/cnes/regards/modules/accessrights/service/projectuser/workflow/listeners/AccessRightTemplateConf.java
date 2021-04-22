package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.listeners;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.templates.domain.Template;
import fr.cnes.regards.modules.templates.service.TemplateConfigUtil;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Configuration
public class AccessRightTemplateConf {

    public static final String USER_DENIED_TEMPLATE_NAME = "USER_DENIED_TEMPLATE";

    public static final String USER_ACTIVATED_TEMPLATE_NAME = "USER_ACTIVATED_TEMPLATE";

    public static final String USER_DISABLED_TEMPLATE_NAME = "USER_DISABLED_TEMPLATE";

    public static final String EMAIL_ACCOUNT_VALIDATION_TEMPLATE_NAME = "EMAIL_ACCOUNT_VALIDATION_TEMPLATE";

    @Bean
    public Template userDeniedTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(USER_DENIED_TEMPLATE_NAME, "template/user-denied-template.html");
    }

    @Bean
    public Template userActivatedTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(USER_ACTIVATED_TEMPLATE_NAME, "template/user-activated-template.html");
    }

    @Bean
    public Template userDisabledTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(USER_DISABLED_TEMPLATE_NAME, "template/user-disabled-template.html");
    }

    @Bean
    public Template emailAccountValidationTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(EMAIL_ACCOUNT_VALIDATION_TEMPLATE_NAME,
            "template/email-account-validation-template.html");
    }

}
