package fr.cnes.regards.modules.accessrights.instance.service.workflow;

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

    public static final String ACCOUNT_REFUSED_TEMPLATE_NAME = "ACCOUNT_REFUSED_TEMPLATE";

    public static final String ACCOUNT_UNLOCK_TEMPLATE_NAME = "ACCOUNT_UNLOCK_TEMPLATE";

    public static final String PASSWORD_RESET_TEMPLATE_NAME = "PASSWORD_RESET_TEMPLATE";

    public static final String PASSWORD_CHANGED_TEMPLATE_NAME = "PASSWORD_CHANGED_TEMPLATE";

    @Bean
    public Template accountRefusedTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(ACCOUNT_REFUSED_TEMPLATE_NAME, "template/account-refused-template.html");
    }

    @Bean
    public Template accountUnlockTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(ACCOUNT_UNLOCK_TEMPLATE_NAME, "template/account-unlock-template.html");
    }

    @Bean
    public Template passwordResetTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(PASSWORD_RESET_TEMPLATE_NAME, "template/password-reset-template.html");
    }

    @Bean
    public Template passwordChangedTemplate() throws IOException {
        return TemplateConfigUtil.readTemplate(PASSWORD_CHANGED_TEMPLATE_NAME,
                                               "template/password-changed-template.html");
    }

}
