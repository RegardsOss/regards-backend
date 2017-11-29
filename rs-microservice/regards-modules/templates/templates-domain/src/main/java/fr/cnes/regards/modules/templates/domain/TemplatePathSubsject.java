package fr.cnes.regards.modules.templates.domain;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class TemplatePathSubsject {

    /**
     * The template path
     */
    private String templatePath;

    private String emailSubject;

    public TemplatePathSubsject(String templatePath, String emailSubject) {
        this.templatePath = templatePath;
        this.emailSubject = emailSubject;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }
}
