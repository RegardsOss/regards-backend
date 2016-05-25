package fr.cs.regards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
public class Bootstrap {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(Bootstrap.class, args);
    }
}
