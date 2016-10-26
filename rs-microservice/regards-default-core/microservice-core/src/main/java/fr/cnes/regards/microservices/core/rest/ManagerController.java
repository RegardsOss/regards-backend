/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.rest;

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

    @Autowired
    private ApplicationManager applicationManager;

    /**
     * endpoint for immediate shutdown
     */
    @PostMapping("/shutdown/immediate")
    public ResponseEntity<Void> immediateShutdown() {
        applicationManager.immediateShutdown();
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
