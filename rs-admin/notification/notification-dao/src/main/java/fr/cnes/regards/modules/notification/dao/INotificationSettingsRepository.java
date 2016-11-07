/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.dao;

import java.util.stream.Stream;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.notification.domain.NotificationFrequency;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;

/**
 * Interface for an JPA auto-generated CRUD repository managing NotificationSettings.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface INotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    /**
     * Find the notification settings for the passed project user.
     *
     * @param pProjectUser
     *            The project user
     * @return The found notification settings
     */
    NotificationSettings findOneByProjectUser(ProjectUser pProjectUser);

    /**
     * Retrieve all notification configuration parameters with passed frequency
     *
     * @param pFrequency
     *            The notification settings
     * @return The {@link NotificationSettings}
     */
    Stream<NotificationSettings> findByFrequency(NotificationFrequency pFrequency);
}
