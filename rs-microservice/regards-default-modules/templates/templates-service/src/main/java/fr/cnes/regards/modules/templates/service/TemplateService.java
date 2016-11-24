/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.templates.service;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.templates.dao.ITemplateRepository;
import fr.cnes.regards.modules.templates.domain.Template;

/**
 * {@link ITemplateService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@Transactional
public class TemplateService implements ITemplateService {

    /**
     * The JPA repository managing CRUD operation on templates. Autowired by Spring.
     */
    private final ITemplateRepository templateRepository;

    /**
     * Constructor
     *
     * @param pTemplateRepository
     *            the template repository
     */
    public TemplateService(final ITemplateRepository pTemplateRepository) {
        super();
        templateRepository = pTemplateRepository;
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
        final Template toCreate = new Template(pTemplate.getCode(), pTemplate.getContent(), pTemplate.getData());
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
        if (pId != pTemplate.getId()) {
            throw new EntityInconsistentIdentifierException(pId, pTemplate.getId(), Template.class);
        }
        final Template template = findById(pId);
        template.setContent(pTemplate.getContent());
        template.setData(pTemplate.getData());
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

}
