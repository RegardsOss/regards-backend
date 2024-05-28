/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.dao;

import com.google.common.annotations.VisibleForTesting;
import fr.cnes.regards.modules.storage.dao.entity.download.DownloadQuotaLimitsEntity;
import fr.cnes.regards.modules.storage.dao.entity.download.UserDownloadQuotaEntity;
import fr.cnes.regards.modules.storage.dao.entity.download.UserDownloadRateEntity;
import fr.cnes.regards.modules.storage.dao.entity.mapping.DomainEntityMapper;
import fr.cnes.regards.modules.storage.domain.database.*;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DownloadQuotaRepositoryImpl implements IDownloadQuotaRepository {

    public static final String INSTANCE = "instance";

    public static final String EMAIL = "email";

    public static final String COUNTER = "counter";

    public static final String GAUGE = "gauge";

    public static final String EXPIRY = "expiry";

    @Autowired
    private IDownloadQuotaLimitsEntityRepository delegateQuotaLimitsRepo;

    @Autowired
    private IUserDownloadQuotaEntityRepository delegateQuotaRepo;

    @Autowired
    private IUserDownloadRateEntityRepository delegateRateRepo;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DomainEntityMapper mapper;

    @Override
    public DownloadQuotaLimits save(DownloadQuotaLimits quota) {
        return mapper.toDomain(delegateQuotaLimitsRepo.save(mapper.toEntity(quota)));
    }

    @Override
    public Optional<DownloadQuotaLimits> findByEmail(String email) {
        return delegateQuotaLimitsRepo.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Map<String, Long> fetchMaxQuotaByUser() {
        return delegateQuotaLimitsRepo.findAll()
                                      .stream()
                                      .collect(Collectors.toMap(DownloadQuotaLimitsEntity::getEmail,
                                                                DownloadQuotaLimitsEntity::getMaxQuota));
    }

    @Override
    public void deleteByEmail(String email) {
        delegateQuotaLimitsRepo.deleteByEmail(email);
    }

    @Override
    public UserQuotaAggregate fetchDownloadQuotaSum(String email) {
        return delegateQuotaRepo.findByEmail(email)
                                .stream()
                                .reduce(new UserQuotaAggregate(0L),
                                        (userQuotaAggregate, userDownloadQuotaEntity) -> new UserQuotaAggregate(
                                            userQuotaAggregate.getCounter() + userDownloadQuotaEntity.getCounter()),
                                        (left, right) -> left);
    }

    @Override
    public UserRateAggregate fetchDownloadRatesSum(String email) {
        return delegateRateRepo.findByEmail(email)
                               .stream()
                               .reduce(new UserRateAggregate(0L),
                                       (userRateAggregate, userDownloadRateEntity) -> new UserRateAggregate(
                                           userRateAggregate.getGauge() + userDownloadRateEntity.getGauge()),
                                       (left, right) -> left);
    }

    @Override
    public UserDownloadQuota upsertOrCombineDownloadQuota(String instanceId, String email, Long diff) {
        // rather than implementing sync issues in code, we use DB to manage sync conflict as it is the only truth source (INSERT ... ON CONFLICT ... DO UPDATE)
        entityManager.createNativeQuery("INSERT INTO {h-schema}t_user_download_quota_counter AS c "
                                        + " (id, instance_id, email, counter) "
                                        + " VALUES (nextval('{h-schema}seq_download_quota_counter'), :instance, :email, :counter) "
                                        +
                                        // FIXME:Right now nextval increment by 50 each time. Check if we can update existing sequence in flyway sql to change increment from 50 to 1.
                                        " ON CONFLICT (instance_id, email) "
                                        + " DO UPDATE "
                                        + " SET counter  = c.counter + EXCLUDED.counter "
                                        +
                                        // c.counter is value in DB right now, EXCLUDED.counter is value we wanted to set
                                        " RETURNING *", UserDownloadQuotaEntity.class)
                     .setParameter(INSTANCE, instanceId)
                     .setParameter(EMAIL, email)
                     .setParameter(COUNTER, diff)
                     .getSingleResult();

        // RETURNING(from previous query) returns only id so we have to query the actual value
        UserDownloadQuotaEntity entity = (UserDownloadQuotaEntity) entityManager.createNativeQuery(
                                                                                    "SELECT * FROM {h-schema}t_user_download_quota_counter "
                                                                                    + " WHERE instance_id  = :instance "
                                                                                    + "   AND email        = :email",
                                                                                    UserDownloadQuotaEntity.class)
                                                                                .setParameter(INSTANCE, instanceId)
                                                                                .setParameter(EMAIL, email)
                                                                                .getSingleResult();

        return mapper.toDomain(entity);
    }

    @Override
    public UserDownloadRate upsertOrCombineDownloadRate(String instanceId,
                                                        String email,
                                                        Long diff,
                                                        LocalDateTime expiry) {
        // rather than implementing sync issues in code, we use DB to manage sync conflict as it is the only truth source (INSERT ... ON CONFLICT ... DO UPDATE)
        entityManager.createNativeQuery("INSERT INTO {h-schema}t_user_download_rate_gauge AS r "
                                        + " (id, instance_id, email, gauge, expiry) "
                                        + " VALUES (nextval('{h-schema}seq_download_rate_gauge'), :instance, :email, :gauge, :expiry) "
                                        +
                                        // FIXME:Right now nextval increment by 50 each time. Check if we can update existing sequence in flyway sql to change increment from 50 to 1.
                                        " ON CONFLICT (instance_id, email) "
                                        + " DO UPDATE "
                                        + " SET gauge  = r.gauge + EXCLUDED.gauge "
                                        +
                                        // r.gauge is value in DB right now, EXCLUDED.gauge is value we wanted to set
                                        "   , expiry = EXCLUDED.expiry "
                                        + " RETURNING *", UserDownloadRateEntity.class)
                     .setParameter(INSTANCE, instanceId)
                     .setParameter(EMAIL, email)
                     .setParameter(GAUGE, diff)
                     .setParameter(EXPIRY, expiry)
                     .getSingleResult();

        // RETURNING(from previous query) returns only id so we have to query the actual value
        UserDownloadRateEntity entity = (UserDownloadRateEntity) entityManager.createNativeQuery(
                                                                                  "SELECT * FROM {h-schema}t_user_download_rate_gauge "
                                                                                  + " WHERE instance_id  = :instance "
                                                                                  + "   AND email        = :email",
                                                                                  UserDownloadRateEntity.class)
                                                                              .setParameter(INSTANCE, instanceId)
                                                                              .setParameter(EMAIL, email)
                                                                              .getSingleResult();

        return mapper.toDomain(entity);
    }

    @Override
    public void deleteExpiredRates() {
        delegateRateRepo.deleteAllExpiredSince(LocalDateTime.now());
    }

    @VisibleForTesting
    @Override
    public void deleteAll() {
        delegateQuotaLimitsRepo.deleteAll();
        delegateQuotaRepo.deleteAll();
        delegateRateRepo.deleteAll();
    }
}
