/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.emails.domain;

import org.springframework.hateoas.Identifiable;

/**
 * Domain object for Emails
 *
 * @author xbrochard
 *
 */
public class Email implements Identifiable<Long> {

    /**
     * Id
     */
    private final Long id_;

    /**
     * Creates an {@link Email} with the given id_.
     *
     * @param Long The email id_ 
     */
    public Email(final Long pId) {
        id_ = pId;
    }

    /**
     * Get id_
     *
     * @return The email id_
     */
    @Override
    public Long getId() {
        return id_;
    }

}
