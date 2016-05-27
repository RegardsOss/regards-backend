package fr.cs.cmz.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
public class DownloadApplication {

	public static void main(String[] args) {
		SpringApplication.run(DownloadApplication.class, args);
	}

	@Bean
	public HttpMessageConverters customConverters() {
		ByteArrayHttpMessageConverter arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
		return new HttpMessageConverters(arrayHttpMessageConverter);
	}
	
	@Configuration
	public static class WebConfig extends WebMvcConfigurerAdapter {

		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			configurer.setDefaultTimeout(3600000);
			configurer.setTaskExecutor(asyncTaskExecutor());
		}
		
		@Bean
		public AsyncTaskExecutor asyncTaskExecutor() {
			return new SimpleAsyncTaskExecutor("toto");
		}
		
	}
}
