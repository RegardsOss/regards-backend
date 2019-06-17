package fr.cnes.regards.modules.storage.service.job;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.MimeTypeUtils;

import fr.cnes.regards.framework.microservice.maintenance.MaintenanceException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.storage.dao.AIPQueryGenerator;
import fr.cnes.regards.modules.storage.dao.IAIPDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.PartialDeletionReport;
import fr.cnes.regards.modules.storage.domain.job.AIPQueryFilters;
import fr.cnes.regards.modules.storage.service.IAIPService;
import fr.cnes.regards.modules.storage.service.StorageTemplateConfiguration;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DeleteFilesFromDataStorageJob extends AbstractJob<Void> {

    public static final String FILTER_PARAMETER_NAME = "filter";

    public static final String DATA_STORAGE_ID_PARAMETER_NAME = "dataStorageId";

    @Autowired
    private IAIPService aipService;

    @Autowired
    private ITemplateService templateService;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private IAIPDao aipDao;

    /**
     * Number of created AIPs processed on each iteration by project
     */
    @Value("${regards.storage.aips.iteration.limit:100}")
    private Integer aipIterationLimit;

    private Map<String, JobParameter> parameters;

    private Page<AIP> aipsPage;

    @Override
    public void run() {
        AIPQueryFilters filters = parameters.get(FILTER_PARAMETER_NAME).getValue();
        Pageable pageRequest = PageRequest.of(0, aipIterationLimit, Sort.Direction.ASC, "id");
        PartialDeletionReport report = new PartialDeletionReport(
                parameters.get(DATA_STORAGE_ID_PARAMETER_NAME).getValue());
        do {
            aipsPage = aipDao.findAll(AIPQueryGenerator
                    .searchAIPContainingAllTags(filters.getState(), filters.getFrom(), filters.getTo(),
                                                filters.getTags(), filters.getSession(), filters.getProviderId(),
                                                filters.getAipIds(), filters.getAipIdsExcluded(),
                                                filters.getStoredOn()),
                                      pageRequest);

            report.append(aipService
                    .deleteFilesFromDataStorage(aipsPage.getContent().stream().map(aip -> aip.getId().toString())
                            .collect(Collectors.toSet()), parameters.get(DATA_STORAGE_ID_PARAMETER_NAME).getValue()));
            advanceCompletion();
            pageRequest = aipsPage.nextPageable();
        } while (aipsPage.hasNext());

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("report", report);
        dataMap.put("filters", filters.toString());
        // lets use the template service to get our message
        String message;
        try {
            message = templateService.render(StorageTemplateConfiguration.PARTIAL_DELETION_REPORT_TEMPLATE_NAME,
                                             dataMap);
        } catch (TemplateException e) {
            throw new MaintenanceException(e.getMessage(), e);
        }
        notificationClient.notify(message, "REGARDS - Partial deletion report", NotificationLevel.INFO,
                                  MimeTypeUtils.TEXT_HTML, DefaultRole.ADMIN);
    }

    @Override
    public boolean needWorkspace() {
        return false;
    }

    @Override
    public int getCompletionCount() {
        return aipsPage.getTotalPages();
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        checkParameters(parameters);
        this.parameters = parameters;
    }

    private void checkParameters(Map<String, JobParameter> parameters)
            throws JobParameterInvalidException, JobParameterMissingException {
        JobParameter dataStorageIdParam = parameters.get(DATA_STORAGE_ID_PARAMETER_NAME);
        if (dataStorageIdParam == null) {
            JobParameterMissingException e = new JobParameterMissingException(
                    String.format("Job %s: parameter %s not provided", this.getClass().getName(),
                                  DATA_STORAGE_ID_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
        if (!Long.class.isAssignableFrom(dataStorageIdParam.getValue().getClass())) {
            JobParameterInvalidException e = new JobParameterInvalidException(
                    String.format("Job %s: parameter %s should be of type %s", this.getClass().getName(),
                                  DATA_STORAGE_ID_PARAMETER_NAME, Long.class.getName()));
            logger.error(e.getMessage(), e);
            throw e;
        }
        JobParameter filterParam = parameters.get(FILTER_PARAMETER_NAME);
        if (filterParam == null) {

            JobParameterMissingException e = new JobParameterMissingException(String
                    .format("Job %s: parameter %s not provided", this.getClass().getName(), FILTER_PARAMETER_NAME));
            logger.error(e.getMessage(), e);
            throw e;
        }
        // Check if the filterParam can be correctly parsed, depending of its type
        if (!(filterParam.getValue() instanceof AIPQueryFilters)) {
            JobParameterInvalidException e = new JobParameterInvalidException(
                    String.format("Job %s: parameter %s should be of type %s", this.getClass().getName(),
                                  FILTER_PARAMETER_NAME, AIPQueryFilters.class.getName()));
            logger.error(e.getMessage(), e);
            throw e;
        }
    }
}
