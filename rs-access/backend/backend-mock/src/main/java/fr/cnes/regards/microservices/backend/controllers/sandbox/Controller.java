package fr.cnes.regards.microservices.backend.controllers.sandbox;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.annotation.ModuleInfo;

@RestController
@ModuleInfo(name = "controller", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
public class Controller {

    @Autowired
    private SimpMessagingTemplate template;

    /**
     * Method to send current date to web socket clients
     */
    public void sendTime() {
        Date now = new Date();
        System.out.println("Sending time to websocket");
        // Send time to each client connected
        this.template.convertAndSend("/topic/time", now.toString());
    }

}
