package fr.cnes.regards.microservices.backend.controllers.sandbox;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
// Indicates that those resources are securised. Only the /oauth endpoint do not
// need the authentication token
@EnableResourceServer
@RequestMapping("/api")
public class Controller {

    @Autowired
    private SimpMessagingTemplate template;

    /**
     * Method to send curent date to web socket clients
     */
    public void sendTime() {
        Date now = new Date();
        System.out.println("Sending time to websocket");
        // Send time to each client connected
        this.template.convertAndSend("/topic/time", now.toString());
    }

}
