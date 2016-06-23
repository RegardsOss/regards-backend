package poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.google.common.base.Predicate;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import static springfox.documentation.builders.PathSelectors.regex;


@SpringBootApplication
@EnableSwagger2 //Enable swagger 2.0 spec
public class Application {
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Bean
    public Docket petApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("poc-api")
                .apiInfo(apiInfo())
                .select()
                .paths(apiPaths())
                .build();
//                .securitySchemes(newArrayList(oauth()))
//                .securityContexts(newArrayList(securityContext()
    }
    
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Springfox POC API")
                .description("API de test pour springfox")
                .termsOfServiceUrl("http://localhost:8080")
                .license("Apache License Version 2.0")
                .version("0.0.1")
                .build();
    }
    
    private Predicate<String> apiPaths() {
        return regex("/api.*");
    }

}
