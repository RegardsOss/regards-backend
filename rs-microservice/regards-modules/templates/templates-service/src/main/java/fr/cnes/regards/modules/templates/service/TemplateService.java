/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.templates.service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.templates.dao.ITemplateRepository;
import fr.cnes.regards.modules.templates.domain.Template;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

/**
 * {@link ITemplateService} implementation.
 * @author Xavier-Alexandre Brochard
 * @author Marc Sordi
 */
@Service
public class TemplateService implements ITemplateService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(TemplateService.class);

    /**
     * Freemarker version-major number. Needed for configuring the Freemarker library.
     * @see {@link Configuration#Configuration(Version)}
     */
    private static final int INCOMPATIBLE_IMPROVEMENTS_VERSION_MAJOR = 2;

    /**
     * Freemarker version-minor number. Needed for configuring the Freemarker library.
     * @see {@link Configuration#Configuration(Version)}
     */
    private static final int INCOMPATIBLE_IMPROVEMENTS_VERSION_MINOR = 3;

    /**
     * Freemarker version-micro number. Needed for configuring the Freemarker library.
     * @see {@link Configuration#Configuration(Version)}
     */
    private static final int INCOMPATIBLE_IMPROVEMENTS_VERSION_MICRO = 25;

    /**
     * The JPA repository managing CRUD operation on templates. Autowired by Spring.
     */
    @Autowired
    private ITemplateRepository templateRepository;

    /**
     * The string template loader
     */
    private StringTemplateLoader loader;

    /**
     * The freemarker configuration
     */
    private Configuration configuration;

    /**
     * Tenant resolver to access all configured tenant
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private Template passwordResetTemplate;

    @Autowired
    private Template accountUnlockTemplate;

    @Autowired
    private Template accountRefusedTemplate;

    @Autowired
    @Resource(name = TemplateServiceConfiguration.TEMPLATES)
    private List<Template> templates;

    @Value("${spring.mail.sender.no.reply:regards@noreply.fr}")
    private String noReplyAdress;

    @Value("${spring.application.name}")
    private String microserviceName;

    public TemplateService() throws IOException {
        configureTemplateLoader();
    }

    /**
     * Init medthod
     */
    @PostConstruct
    public void init() {
        for (final String tenant : tenantResolver.getAllActiveTenants()) {
            // Set working tenant
            runtimeTenantResolver.forceTenant(tenant);
            // Init default templates for this tenant
            initDefaultTemplates();
        }
    }

    @EventListener
    public void processEvent(TenantConnectionReady event) {
        // Set working tenant
        runtimeTenantResolver.forceTenant(event.getTenant());
        // Init default templates for this tenant
        initDefaultTemplates();
    }

    /**
     * Populate the templates with default
     */
    private void initDefaultTemplates() {
        // Look into classpath (via TemplateServiceConfiguration) if some templates are present. If yes, check if they
        // exist into Database, if not, create them
        for (Template template : templates) {
            checkAndSaveIfNecessary(template);
        }
    }

    private void checkAndSaveIfNecessary(Template template) {
        if ((template != null) && !templateRepository.findOneByCode(template.getCode()).isPresent()) {
            templateRepository.save(template);
        }
    }

    @Override
    public List<Template> findAll() {
        return templateRepository.findAll();
    }

    @Override
    public Template create(final Template template) {
        final Template toCreate = new Template(template.getCode(), template.getContent(), template.getDataStructure(),
                                               template.getSubject());
        return templateRepository.save(toCreate);
    }

    @Override
    public Template findById(final Long id) throws EntityNotFoundException {
        final Optional<Template> template = Optional.ofNullable(templateRepository.findOne(id));
        return template.orElseThrow(() -> new EntityNotFoundException(id, Template.class));
    }

    @Override
    public void update(final Long id, Template template) throws EntityException {
        if (!id.equals(template.getId())) {
            throw new EntityInconsistentIdentifierException(id, template.getId(), Template.class);
        }
        template = findById(id);
        template.setContent(template.getContent());
        template.setDataStructure(template.getDataStructure());
        template.setDescription(template.getDescription());

        templateRepository.save(template);
    }

    @Override
    public void delete(final Long id) throws EntityNotFoundException {
        if (!templateRepository.exists(id)) {
            throw new EntityNotFoundException(id, Template.class);
        }
        templateRepository.delete(id);
    }

    @Override
    public void deleteAll() {
        templateRepository.deleteAll();
    }

    /**
     * Configure the template loader
     * @throws IOException when error occurs during template loading
     */
    private void configureTemplateLoader() throws IOException {
        configuration = new Configuration(
                new Version(INCOMPATIBLE_IMPROVEMENTS_VERSION_MAJOR, INCOMPATIBLE_IMPROVEMENTS_VERSION_MINOR,
                            INCOMPATIBLE_IMPROVEMENTS_VERSION_MICRO));
        loader = new StringTemplateLoader();
        configuration.setTemplateLoader(loader);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    @Override
    public SimpleMailMessage writeToEmail(String templateCode, String subject, Map<String, ? extends Object> dataModel,
            String... recipients) throws EntityNotFoundException {
        // Retrieve the template of given code
        Template template = null;
        if (!runtimeTenantResolver.isInstance()) {
            template = templateRepository.findOneByCode(templateCode)
                    .orElseThrow(() -> new EntityNotFoundException(templateCode, Template.class));
        } else { // On instance, no access to project databases so templates are managed by hand
            if (accountUnlockTemplate.getCode().equals(templateCode)) {
                template = accountUnlockTemplate;
            } else if (passwordResetTemplate.getCode().equals(templateCode)) {
                template = passwordResetTemplate;
            } else if (accountRefusedTemplate.getCode().equals(templateCode)) {
                template = accountRefusedTemplate;
            }
            if (template == null) {
                throw new EntityNotFoundException(templateCode, Template.class);
            }
        }

        // Add the template (regards template POJO) to the loader
        loader.putTemplate(template.getCode(), template.getContent());

        // Define the email message
        String text;
        try {
            final Writer out = new StringWriter();
            // Retrieve the template (freemarker Template) and process it with the data model
            configuration.getTemplate(template.getCode()).process(dataModel, out);
            text = out.toString();
        } catch (TemplateException | IOException e) {
            LOG.warn("Unable to process the data into the template of code " + template.getCode()
                             + ". Falling back to the not templated content.", e);
            text = template.getContent();
        }

        final SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject((subject == null) ? template.getSubject() : subject);
        message.setText(text);
        message.setTo(recipients);
        message.setFrom(noReplyAdress);

        return message;
    }

}
