/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.templates.dao.ITemplateRepository;
import fr.cnes.regards.modules.templates.domain.Template;
import freemarker.cache.StringTemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * {@link ITemplateService} implementation.
 * @author Xavier-Alexandre Brochard
 * @author Marc Sordi
 */
@Service
@RegardsTransactional
public class TemplateService implements ITemplateService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(TemplateService.class);

    /**
     * Instance microservice type
     */
    private static final String MICROSERVICE_TYPE = "instance";

    /**
     * The JPA repository managing CRUD operation on templates. Autowired by Spring.
     */
    @Autowired
    private ITemplateRepository templateRepository;

    /**
     * The freemarker configuration
     */
    private final Configuration configuration;

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
    private Set<Template> templates;

    @Value("${regards.microservice.type:multitenant}")
    private String microserviceType;

    public TemplateService() {
        // Configure Freemarker
        configuration = new Configuration(Configuration.VERSION_2_3_25);

        configuration.setTemplateLoader(new StringTemplateLoader());
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
    }

    /**
     * Init method
     */
    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationStarted(ApplicationStartedEvent event) {
        if (microserviceType.equals(MICROSERVICE_TYPE)) {
            // Init default templates for this tenant
            initDefaultTemplates();
        } else {
            for (final String tenant : tenantResolver.getAllActiveTenants()) {
                // Set working tenant
                runtimeTenantResolver.forceTenant(tenant);
                // Init default templates for this tenant
                initDefaultTemplates();
            }
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onTenantConnectionReady(TenantConnectionReady event) {
        // Set working tenant
        runtimeTenantResolver.forceTenant(event.getTenant());
        // Init default templates for this tenant
        initDefaultTemplates();
    }

    /**
     * Populate the templates with default
     */
    @Override
    public void initDefaultTemplates() {
        // Look into classpath (via TemplateConfigUtil) if some templates are present. If yes, check if they
        // exist into Database, if not, create them
        for (Template template : templates) {
            if (!templateRepository.findByName(template.getName()).isPresent()) {
                // Add the template (regards template POJO) to the loader
                templateRepository.save(template);
            }
        }
    }

    @Override
    public List<Template> findAll() {
        return templateRepository.findAll();
    }

    @Override
    public Template findById(Long id) throws EntityNotFoundException {
        final Optional<Template> template = templateRepository.findById(id);
        return template.orElseThrow(() -> new EntityNotFoundException(id, Template.class));
    }

    @Override
    public Template update(Long id, Template template)
            throws EntityInconsistentIdentifierException, EntityNotFoundException, EntityInvalidException {
        if (!id.equals(template.getId())) {
            throw new EntityInconsistentIdentifierException(id, template.getId(), Template.class);
        }
        if (!templateRepository.existsById(id)) {
            throw new EntityNotFoundException(id, Template.class);
        }
        try {
            //try to parse the template to be sure that user didn't give us some bad things
            getTemplateLoader().putTemplate(template.getName(), template.getContent());
            configuration.getTemplate(template.getName());
        } catch (ParseException e) {
            throw new EntityInvalidException("Given template content could not be parsed by Freemarker.", e);
        } catch (IOException e) {
            LOG.error("Template could not be retrieve from Freemarker configuration", e);
            throw new RsRuntimeException(e);
        }
        return templateRepository.save(template);
    }

    @Override
    public String render(String templateName, Map<String, ?> dataModel) throws TemplateException {
        // Retrieve the template of given code
        Template template = templateRepository.findByName(templateName).get();

        getTemplateLoader().putTemplate(templateName, template.getContent());
        try {
            final Writer out = new StringWriter();
            // Retrieve the template (freemarker Template) and process it with the data model
            configuration.getTemplate(template.getName()).process(dataModel, out);
            return out.toString();
        } catch (IOException e) {
            LOG.error("Unable to process the data into the template of code " + template.getName()
                    + ". Falling back to the not templated content.", e);
            throw new RsRuntimeException(e);
        }
    }

    private StringTemplateLoader getTemplateLoader() {
        return (StringTemplateLoader) configuration.getTemplateLoader();
    }

}
