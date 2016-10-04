/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.domain;

import org.springframework.hateoas.Identifiable;

/**
 * Domain object for emails' recipients
 *
 * @author xbrochard
 *
 */
public class Recipient implements Identifiable<Long> {

    /**
     * Id
     */
    private final Long id_;

    /**
     * Creates an {@link Email} with the given id_.
     *
     * @param Long
     *            The email id_
     */
    public Recipient(final Long pId) {
        id_ = pId;
    }

    /**
     * Get id_
     *
     * @return The recipient id_
     */
    @Override
    public Long getId() {
        return id_;
    }

}
