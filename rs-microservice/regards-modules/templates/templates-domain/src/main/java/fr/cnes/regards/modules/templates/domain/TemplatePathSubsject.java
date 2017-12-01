package fr.cnes.regards.modules.templates.domain;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class TemplatePathSubsject {

    /**
     * The template path
     */
    private String templatePath;

    /**
     * The email subject
     */
    private String emailSubject;

    /**
     * Constructor setting the parameters as attribute
     * @param templatePath
     * @param emailSubject
     */
    public TemplatePathSubsject(String templatePath, String emailSubject) {
        this.templatePath = templatePath;
        this.emailSubject = emailSubject;
    }

    /**
     * @return the template path
     */
    public String getTemplatePath() {
        return templatePath;
    }

    /**
     * Set the template path
     * @param templatePath
     */
    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    /**
     * @return the email subject
     */
    public String getEmailSubject() {
        return emailSubject;
    }

    /**
     * Set the subject of the email
     * @param emailSubject
     */
    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }
}
