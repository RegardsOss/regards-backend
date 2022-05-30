package fr.cnes.regards.modules.feature.service.session;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@MultitenantTransactional
//FIXME naming is error prone and FeatureSessionDeleteService already exist with only one method! go merge those 2 classes!
public class FeatureDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureDeleteService.class);

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private FeatureConfigurationProperties properties;

    @Autowired
    private IFeatureEntityRepository featureEntityRepository;

    @Autowired
    private IPublisher publisher;

    public void scheduleDeletion(String source, Optional<String> session) {
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(PriorityLevel.NORMAL.getPriorityLevel());
        jobInfo.setParameters(FeatureDeleteJob.getParameters(source, session));
        jobInfo.setClassName(FeatureDeleteJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
    }

    public long scheduleDeleteRequests(String source, String session) {
        long deletedFeaturesCount = 0;
        Page<ILightFeatureEntity> entityPage;
        Pageable pageable = PageRequest.of(0, properties.getMaxBulkSize(), Sort.by("id"));
        int requestCount = 0;
        String deletionOwner;
        if (session == null || session.length() == 0) {
            deletionOwner = "delete-job-" + source;
        } else {
            deletionOwner = "delete-job-" + source + "-" + session;
        }
        do {
            if (StringUtils.isEmpty(session)) {
                entityPage = featureEntityRepository.findBySessionOwner(source, pageable);
            } else {
                entityPage = featureEntityRepository.findBySessionOwnerAndSession(source, session, pageable);
            }
            List<FeatureUniformResourceName> featureUrns = entityPage.stream()
                                                                     .map(ILightFeatureEntity::getUrn)
                                                                     .collect(Collectors.toList());
            List<FeatureDeletionRequestEvent> events = Lists.newArrayList();
            for (FeatureUniformResourceName urn : featureUrns) {
                FeatureDeletionRequestEvent event = FeatureDeletionRequestEvent.build(deletionOwner,
                                                                                      urn,
                                                                                      PriorityLevel.NORMAL);
                events.add(event);
                requestCount++;
            }
            publisher.publish(events);
            pageable = entityPage.nextPageable();
        } while (!Thread.currentThread().isInterrupted() && entityPage.hasNext());

        LOGGER.info("[SESSION DELETE EVENT] {} delete request created for source {} and session {}",
                    requestCount,
                    source,
                    session);

        if (Thread.currentThread().isInterrupted()) {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
            Thread.currentThread().interrupt();
        }

        return deletedFeaturesCount;
    }

}
