package com.example.prout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// a décommenter pour paraméter à la mano les serveurs
//@OpenAPIDefinition(servers = { @Server(url = "http://localhost:8080"), @Server(url = "http://localhost:8081") },
//        info = @Info(title = "the title", version = "v1", description = "My API",
//                license = @License(name = "Apache 2.0", url = "http://foo.bar"),
//                contact = @Contact(url = "http://gigantic-server.com", name = "Fred",
//                        email = "Fred@gigagantic-server.com")))
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
