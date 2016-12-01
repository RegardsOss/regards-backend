/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JwtTokenUtils;
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
 */
@Service
@MultitenantTransactional
@ContextConfiguration(classes = { TemplateServiceConfiguration.class })
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

    @Autowired
    private Template emailValidationTemplate;

    @Autowired
    private Template passwordResetTemplate;

    /**
     * Constructor
     *
     * @param pTemplateRepository
     *            the template repository
     * @param pTenantResolver
     *            the tenant resolver
     * @throws IOException
     *             when an error occurs while configuring the template loader
     */
    public TemplateService(final ITemplateRepository pTemplateRepository, final ITenantResolver pTenantResolver)
            throws IOException {
        super();
        templateRepository = pTemplateRepository;
        tenantResolver = pTenantResolver;
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

        final Supplier<Void> createDefaultTemplates = () -> {
            if (!templateRepository.findOneByCode(emailValidationTemplate.getCode()).isPresent()) {
                templateRepository.save(emailValidationTemplate);
            }
            if (!templateRepository.findOneByCode(passwordResetTemplate.getCode()).isPresent()) {
                templateRepository.save(passwordResetTemplate);
            }
            return null;
        };

        final Function<String, Void> createDefaultTemplatesOnTenant = JwtTokenUtils
                .asSafeCallableOnTenant(createDefaultTemplates);

        // For each tenant, create default templates if needed
        try (Stream<String> tenantsStream = tenantResolver.getAllTenants().stream()) {
            tenantsStream.forEach(createDefaultTemplatesOnTenant::apply);
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
        final Template template = templateRepository.findOneByCode(pTemplateCode)
                .orElseThrow(() -> new EntityNotFoundException(pTemplateCode, Template.class));

        // Add the template (regards template POJO) to the loader
        loader.putTemplate(template.getCode(), template.getContent());

        // Define the email message
        String text;
        try {
            final Writer out = new StringWriter();
            // Retrieve the template (freemarker Template) and process it with the data model
            configuration.getTemplate(template.getCode()).process(pDataModel, out);
            text = out.toString();
        } catch (TemplateException | IOException e) {
            LOG.warn("Unable to process the data into the template of code " + template.getCode()
                    + ". Falling back to the not templated content.", e);
            text = template.getContent();
        }

        final SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(template.getSubject());
        message.setText(text);
        message.setTo(pRecipients);
        // TODO
        message.setFrom("system@regards.com");

        return message;
    }

}
