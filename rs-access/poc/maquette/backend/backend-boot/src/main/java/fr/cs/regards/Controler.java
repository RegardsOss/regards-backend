package fr.cs.regards;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
public class Controler {

	@CrossOrigin(origins = "*")
    @RequestMapping("/controler")
    public String index() {
        return "Greetings from Spring Boot!";
    }

}
