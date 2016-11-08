/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.rest;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.microservice.manager.IApplicationManager;

/**
 * @author svissier
 *
 */
@RestController
@RequestMapping("/")
public class ManagerController {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ManagerController.class);

    /**
     * Application manager
     */
    @Autowired
    private IApplicationManager applicationManager;

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
