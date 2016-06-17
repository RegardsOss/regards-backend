package fr.cs.regards.tools.elastic;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public abstract class ElasticUtils {
	private static final String URL_INDEX_TPL = "http://localhost:9200/%s/";
	private static final String URL_TYPE_TPL = "http://localhost:9200/%s/%s/%d/";

	private static RestTemplate createRestTemplate(){
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
		return restTemplate;
	}
	public static RestTemplate restTemplate = createRestTemplate();
	
	public static <T> void insertObject(String index, String type, int id, T object) {
		String url = String.format(URL_TYPE_TPL, index, type, id);
		put(url, object);
	}


	public static <T> void put(String url, T object) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(new MediaType("application", "json"));
		HttpEntity<T> requestEntity = new HttpEntity<T>(object, requestHeaders);
		
		restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
	}
	

	public static boolean indexExists(String index) {
		try {
			restTemplate.headForHeaders(String.format(URL_INDEX_TPL, index));
		} catch (RestClientException e) {
			return false;
		}
		return true;
	}
	
	public static void createIndex(String index) {
		if (!indexExists(index)) {
			restTemplate.put(String.format(URL_INDEX_TPL, index), null);
		}
	}
}
