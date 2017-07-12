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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

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
 *
 * @author Xavier-Alexandre Brochard
 * @author Marc Sordi
 */
@Service
//@MultitenantTransactional A réactiver
public class TemplateService implements ITemplateService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(TemplateService.class);

    /**
     * Freemarker version-major number. Needed for configuring the Freemarker library.
     *
     * @see {@link Configuration#Configuration(Version)}
     */
    private static final int INCOMPATIBLE_IMPROVEMENTS_VERSION_MAJOR = 2;

    /**
     * Freemarker version-minor number. Needed for configuring the Freemarker library.
     *
     * @see {@link Configuration#Configuration(Version)}
     */
    private static final int INCOMPATIBLE_IMPROVEMENTS_VERSION_MINOR = 3;

    /**
     * Freemarker version-micro number. Needed for configuring the Freemarker library.
     *
     * @see {@link Configuration#Configuration(Version)}
     */
    private static final int INCOMPATIBLE_IMPROVEMENTS_VERSION_MICRO = 25;

    /**
     * The JPA repository managing CRUD operation on templates. Autowired by Spring.
     */
    private final ITemplateRepository templateRepository;

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
    private final ITenantResolver tenantResolver;

    /**
     * Runtime tenant resolver
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private Template emailAccountValidationTemplate;

    @Autowired
    private Template passwordResetTemplate;

    @Autowired
    private Template accountUnlockTemplate;

    @Autowired
    private Template accountRefusedTemplate;

    @Autowired
    private Template projectUserActivatedTemplate;

    @Autowired
    private Template projectUserInactivatedTemplate;

    @Value("${regards.mails.noreply.address:regards@noreply.fr}")
    private String noReplyAdress;

    /**
     *
     * @param pTemplateRepository template repository
     * @param pTenantResolver tenant resolver
     * @param pRuntimeTenantResolver runtime tenant resolver
     * @throws IOException
     */
    public TemplateService(final ITemplateRepository pTemplateRepository, final ITenantResolver pTenantResolver,
            final IRuntimeTenantResolver pRuntimeTenantResolver) throws IOException {
        super();
        templateRepository = pTemplateRepository;
        tenantResolver = pTenantResolver;
        runtimeTenantResolver = pRuntimeTenantResolver;
        configureTemplateLoader();
    }

    /**
     * Init medthod
     */
    @PostConstruct
    public void init() {
        initDefaultTemplates();
    }

    /**
     * Populate the templates with default
     */
    private void initDefaultTemplates() {

        for (final String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            if (!templateRepository.findOneByCode(passwordResetTemplate.getCode()).isPresent()) {
                templateRepository.save(passwordResetTemplate);
            }
            if (!templateRepository.findOneByCode(accountUnlockTemplate.getCode()).isPresent()) {
                templateRepository.save(accountUnlockTemplate);
            }
            if (!templateRepository.findOneByCode(emailAccountValidationTemplate.getCode()).isPresent()) {
                templateRepository.save(emailAccountValidationTemplate);
            }
            if (!templateRepository.findOneByCode(accountRefusedTemplate.getCode()).isPresent()) {
                templateRepository.save(accountRefusedTemplate);
            }
            if (!templateRepository.findOneByCode(projectUserActivatedTemplate.getCode()).isPresent()) {
                templateRepository.save(projectUserActivatedTemplate);
            }
            if (!templateRepository.findOneByCode(projectUserInactivatedTemplate.getCode()).isPresent()) {
                templateRepository.save(projectUserInactivatedTemplate);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.templates.service.ITemplateService#findAll()
     */
    @Override
    public List<Template> findAll() {
        return templateRepository.findAll();
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.templates.service.ITemplateService#create(fr.cnes.regards.modules.templates.domain.
     * Template)
     */
    @Override
    public Template create(final Template pTemplate) {
        final Template toCreate = new Template(pTemplate.getCode(), pTemplate.getContent(),
                pTemplate.getDataStructure(), pTemplate.getSubject());
        return templateRepository.save(toCreate);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.templates.service.ITemplateService#findById(java.lang.Long)
     */
    @Override
    public Template findById(final Long pId) throws EntityNotFoundException {
        final Optional<Template> template = Optional.ofNullable(templateRepository.findOne(pId));
        return template.orElseThrow(() -> new EntityNotFoundException(pId, Template.class));
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.templates.service.ITemplateService#update(java.lang.Long,
     * fr.cnes.regards.modules.templates.domain.Template)
     */
    @Override
    public void update(final Long pId, final Template pTemplate) throws EntityException {
        if (!pId.equals(pTemplate.getId())) {
            throw new EntityInconsistentIdentifierException(pId, pTemplate.getId(), Template.class);
        }
        final Template template = findById(pId);
        template.setContent(pTemplate.getContent());
        template.setDataStructure(pTemplate.getDataStructure());
        template.setDescription(pTemplate.getDescription());

        templateRepository.save(template);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.templates.service.ITemplateService#delete(java.lang.Long)
     */
    @Override
    public void delete(final Long pId) throws EntityNotFoundException {
        if (!templateRepository.exists(pId)) {
            throw new EntityNotFoundException(pId, Template.class);
        }
        templateRepository.delete(pId);
    }

    /**
     * Configure the template loader
     *
     * @throws IOException
     *             when error occurs during template loading
     */
    private void configureTemplateLoader() throws IOException {
        configuration = new Configuration(new Version(INCOMPATIBLE_IMPROVEMENTS_VERSION_MAJOR,
                INCOMPATIBLE_IMPROVEMENTS_VERSION_MINOR, INCOMPATIBLE_IMPROVEMENTS_VERSION_MICRO));
        loader = new StringTemplateLoader();
        configuration.setTemplateLoader(loader);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.templates.service.ITemplateService#write(java.lang.String, java.util.Map,
     * java.lang.String[])
     */
    @Override
    public SimpleMailMessage writeToEmail(final String pTemplateCode, final Map<String, String> pDataModel,
            final String[] pRecipients) throws EntityNotFoundException {
        // Retrieve the template of passed code
        Template template = null;
        if (!runtimeTenantResolver.isInstance()) {
            template = templateRepository.findOneByCode(pTemplateCode)
                    .orElseThrow(() -> new EntityNotFoundException(pTemplateCode, Template.class));
        } else {
            if (accountUnlockTemplate.getCode().equals(pTemplateCode)) {
                template = accountUnlockTemplate;
            }

            if (passwordResetTemplate.getCode().equals(pTemplateCode)) {
                template = passwordResetTemplate;
            }

            if (accountRefusedTemplate.getCode().equals(pTemplateCode)) {
                template = accountRefusedTemplate;
            }

            if (template == null) {
                throw new EntityNotFoundException(pTemplateCode, Template.class);
            }
        }

        // Add the template (regards template POJO) to the loader
        loader.putTemplate(template.getCode(), template.getContent());

        // Define the email message
        String text;
        try {
            final Writer out = new StringWriter();
            // Retrieve the template (freemarker Template) and process it with the data model
            configuration.getTemplate(template.getCode()).process(pDataModel, out);
            text = out.toString();
        } catch (TemplateException |

                IOException e) {
            LOG.warn("Unable to process the data into the template of code " + template.getCode()
                    + ". Falling back to the not templated content.", e);
            text = template.getContent();
        }

        final SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(template.getSubject());
        message.setText(text);
        message.setTo(pRecipients);
        message.setFrom(noReplyAdress);

        return message;
    }

}
