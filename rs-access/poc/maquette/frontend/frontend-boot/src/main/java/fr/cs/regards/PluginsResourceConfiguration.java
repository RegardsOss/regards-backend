package fr.cs.regards;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

// Cette classe permet d'ajouter un répertoire externe pour le chargement des plugins
@Configuration
public class PluginsResourceConfiguration extends WebMvcConfigurerAdapter {

 @Value("${plugins.resource}")
 private String pluginsResource_;
 @Value("${plugins.path}")
 private String pluginsPath_;

 @Override
 public void addResourceHandlers(ResourceHandlerRegistry registry) {
	 if (pluginsPath_ != null){
        registry.addResourceHandler("/"+pluginsResource_+"/**")
        .addResourceLocations("/"+pluginsResource_+"/", "file:"+pluginsPath_);
	 }
 }
}
