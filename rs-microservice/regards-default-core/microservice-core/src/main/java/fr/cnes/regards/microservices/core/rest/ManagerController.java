/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.rest;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.manage.ApplicationManager;

/**
 * @author svissier
 *
 */
@RestController("/")
public class ManagerController {

    private static final Logger LOG = LoggerFactory.getLogger(ManagerController.class);

    @Autowired
    private ApplicationManager applicationManager;

    /**
     * endpoint for immediate shutdown
     */
    @PostMapping("/shutdown/immediate")
    public ResponseEntity<Void> immediateShutdown() {
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        try {
            applicationManager.immediateShutdown();
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
            response = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

}
