package fr.cnes.regards.modules.feature.service.session;


import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.service.conf.FeatureConfigurationProperties;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@MultitenantTransactional
public class FeatureDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureDeleteService.class);

    @Autowired
    private IJobInfoService jobInfoService;
    @Autowired
    private FeatureConfigurationProperties properties;
    @Autowired
    private IFeatureEntityRepository featureEntityRepository;
    @Autowired
    private FeatureSessionNotifier sessionNotifier;

    @Autowired
    private FeatureDeleteService self;

    public void scheduleDeletion(String source, Optional<String> session) {
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(PriorityLevel.NORMAL.getPriorityLevel());
        jobInfo.setParameters(FeatureDeleteJob.getParameters(source, session));
        jobInfo.setClassName(FeatureDeleteJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
    }

    public long delete(String source, String session) {

        long deletedFeaturesCount = 0;
        Page<ILightFeatureEntity> entityPage;
        Pageable pageable = PageRequest.of(0, properties.getMaxBulkSize(), Sort.by("id"));

        do {
            if (StringUtils.isEmpty(session)) {
                entityPage = featureEntityRepository.findBySessionOwner(source, pageable);
            } else {
                entityPage = featureEntityRepository.findBySessionOwnerAndSession(source, session, pageable);
            }
            List<ILightFeatureEntity> entities = entityPage.getContent();
            if (!entities.isEmpty()) {
                self.deleteAndNotify(source, entities);
                deletedFeaturesCount += entities.size();
            }

        } while (!Thread.currentThread().isInterrupted() && entityPage.hasNext());

        if (Thread.currentThread().isInterrupted()) {
            LOGGER.debug("{} thread has been interrupted", this.getClass().getName());
            Thread.currentThread().interrupt();
        }

        return deletedFeaturesCount;
    }

    public void deleteAndNotify(String source, List<ILightFeatureEntity> entities) {

        featureEntityRepository.deleteAllByUrnIn(entities.stream().map(ILightFeatureEntity::getUrn).collect(Collectors.toSet()));

        Map<String, Long> countBySession = entities.stream().collect(Collectors.groupingBy(ILightFeatureEntity::getSession, Collectors.counting()));
        countBySession.forEach((session, count) -> sessionNotifier.decrementCount(source, session, FeatureSessionProperty.REFERENCED_PRODUCTS, count));
    }

}
