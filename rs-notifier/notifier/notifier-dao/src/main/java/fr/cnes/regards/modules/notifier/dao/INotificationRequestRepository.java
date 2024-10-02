/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.dao;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.domain.NotifRequestId;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository to manipulate {@link NotificationRequest}
 *
 * @author Kevin Marchois
 */
@Repository
public interface INotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {

    @Override
    @EntityGraph(attributePaths = { "recipientsScheduled",
                                    "recipientsInError",
                                    "recipientsToSchedule",
                                    "successRecipients",
                                    "rulesToMatch",
                                    "rulesToMatch.recipients" }, type = EntityGraph.EntityGraphType.LOAD)
    List<NotificationRequest> findAllById(Iterable<Long> ids);

    default Page<NotificationRequest> findByState(NotificationState state, Pageable pageable) {
        // asking a page with entity graph is mostly a bad idea because spring jpa & hibernate cannot handle this on there own
        // so first, lets ask identifiers matching the everything without entityGraph then lets request entities thanks to these ids
        Page<NotifRequestId> ids = this.findIdsPageByState(state, pageable);
        List<NotificationRequest> pageContent = findAllById(ids.getContent()
                                                               .stream()
                                                               .map(NotifRequestId::getId)
                                                               .collect(Collectors.toList()));
        return new PageImpl<>(pageContent, pageable, ids.getTotalElements());
    }

    @Query(value = "select nr.id as id from NotificationRequest nr where nr.state = :state",
           countQuery = "select count(nr.id) from NotificationRequest nr where nr.state = :state")
    Page<NotifRequestId> findIdsPageByState(@Param("state") NotificationState state, Pageable pageable);

    /**
     * Update a state according a list of ids
     *
     * @param state {@link NotificationState} to set
     * @param ids   of {@link NotificationRequest}
     */
    @Modifying
    @Query("Update NotificationRequest notif set notif.state = :state Where notif.id in :ids")
    void updateState(@Param("state") NotificationState state, @Param("ids") Set<Long> ids);

    default Page<NotificationRequest> findPageByStateAndRecipientsToScheduleContaining(NotificationState state,
                                                                                       PluginConfiguration recipient,
                                                                                       Pageable pageable) {
        // asking a page with entity graph is mostly a bad idea because spring jpa & hibernate cannot handle this on there own
        // so first, lets ask identifiers matching the everything without entityGraph then lets request entities thanks to these ids
        Page<NotifRequestId> ids = this.findIdsPageByStateAndRecipientsToScheduleContaining(state, recipient, pageable);
        List<NotificationRequest> pageContent = findAllById(ids.getContent()
                                                               .stream()
                                                               .map(NotifRequestId::getId)
                                                               .collect(Collectors.toList()));
        return new PageImpl<>(pageContent, pageable, ids.getTotalElements());
    }

    @Query(value = "select nr.id as id from NotificationRequest nr where nr.state = :state and :recipient member of nr.recipientsToSchedule",
           countQuery = "select count(nr.id) from NotificationRequest nr where nr.state = :state and :recipient member of nr.recipientsToSchedule")
    Page<NotifRequestId> findIdsPageByStateAndRecipientsToScheduleContaining(@Param("state") NotificationState state,
                                                                             @Param("recipient")
                                                                             PluginConfiguration recipient,
                                                                             Pageable pageable);

    /**
     * Retrieve requests with no recipients scheduled. That means that all recipients are processed with success or error
     * result for each one. Those requests are interpreted as completed.
     *
     * @param state    states to search for request completed
     * @param pageable pagination information
     * @return completed requests
     */
    @Query(value = "select notification_.*"
                   + " from t_notification_request notification_ "
                   + " where "
                   + "    (notification_.state = 'SCHEDULED')"
                   + "    and  not ("
                   + "    exists (select notification_.id"
                   + "        from ta_notif_request_recipients_scheduled scheduled"
                   + "        where notification_.id=scheduled.notification_request_id)"
                   + "    )"
                   + "    and  not ("
                   + "    exists (select notification_.id"
                   + "        from ta_notif_request_recipients_toschedule toschedule"
                   + "        where notification_.id=toschedule.notification_request_id)"
                   + "    )"
                   + "    and  not ("
                   + "    exists (select notification_.id"
                   + "            from ta_notif_request_rules_to_match rules"
                   + "            where notification_.id=rules.notification_request_id)"
                   + "        ) limit :max_results", nativeQuery = true)
    List<NotificationRequest> findCompletedRequests(@Param("max_results") int maxResults);

    @EntityGraph(attributePaths = { "recipientsScheduled",
                                    "recipientsInError",
                                    "recipientsToSchedule",
                                    "rulesToMatch" }, type = EntityGraph.EntityGraphType.LOAD)
    Set<NotificationRequest> findAllByRequestIdIn(Set<String> requestsIds);

    @Modifying
    @Query("DELETE FROM NotificationRequest nr WHERE nr.id IN (?1)")
    void deleteByIdIn(List<Long> ids);

    @Modifying
    @Query(value =
               "DELETE from ta_notif_request_rules_to_match where notification_request_id = :requestId and rule_id in "
               + "(:ruleIds)", nativeQuery = true)
    void removeRulesToMatch(@Param("requestId") Long requestId, @Param("ruleIds") List<Long> ruleIds);

    @Modifying
    @Query(value = "DELETE from ta_notif_request_rules_to_match where notification_request_id in (:requestIds)",
           nativeQuery = true)
    void removeRulesToMatch(@Param("requestIds") Collection<Long> requestIds);

    @Modifying
    @Query(value = "insert into ta_notif_request_recipients_toschedule (notification_request_id, recipient_id) values "
                   + "(:requestId, :recipientId)", nativeQuery = true)
    void addRecipientToSchedule(@Param("requestId") Long requestId, @Param("recipientId") Long recipientId);

    @Modifying
    @Query(value = "DELETE from ta_notif_request_recipients_toschedule where notification_request_id in (:requestIds)"
                   + " and recipient_id = :recipientId", nativeQuery = true)
    void removeRecipientToScheduleForRequestIds(@Param("requestIds") Set<Long> requestsIds,
                                                @Param("recipientId") Long recipientId);

    @Modifying
    @Query(value = "insert into ta_notif_request_recipients_scheduled (notification_request_id, recipient_id) values "
                   + "(:requestId, :recipientId)", nativeQuery = true)
    void addRecipientScheduled(@Param("requestId") Long requestId, @Param("recipientId") Long recipientId);

    @Modifying
    @Query(value = "DELETE from ta_notif_request_recipients_scheduled where notification_request_id in (:requestIds)"
                   + " and recipient_id = :recipientId", nativeQuery = true)
    void removeRecipientsScheduledForRequestIds(@Param("requestIds") Set<Long> requestsIds,
                                                @Param("recipientId") Long recipientId);

    @Modifying
    @Query(value = "DELETE from ta_notif_request_recipients_scheduled where notification_request_id = :requestId"
                   + " and recipient_id = :recipientId", nativeQuery = true)
    void removeRecipientScheduled(@Param("requestId") Long requestId, @Param("recipientId") Long recipientId);

    @Modifying
    @Query(value = "insert into ta_notif_request_recipients_success (notification_request_id, recipient_id) values "
                   + "(:requestId, :recipientId)", nativeQuery = true)
    void addRecipientInSuccess(@Param("requestId") Long requestId, @Param("recipientId") Long recipientId);

    @Modifying
    @Query(value = "insert into ta_notif_request_recipients_error (notification_request_id, recipient_id) values "
                   + "(:requestId, :recipientId)", nativeQuery = true)
    void addRecipientInError(@Param("requestId") Long requestId, @Param("recipientId") Long recipientId);

    @Modifying
    @Query(value = "DELETE from ta_notif_request_recipients_error where notification_request_id = :requestId and "
                   + "recipient_id in (:recipientIds)", nativeQuery = true)
    void removeRecipientErrors(@Param("requestId") Long requestId,
                               @Param("recipientIds") Collection<Long> recipientIds);

}
