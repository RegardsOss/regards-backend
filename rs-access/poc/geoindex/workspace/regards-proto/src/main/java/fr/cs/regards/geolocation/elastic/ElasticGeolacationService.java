package fr.cs.regards.geolocation.elastic;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.geojson.Feature;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cs.regards.geolocation.Circle;
import fr.cs.regards.geolocation.Coordinate;
import fr.cs.regards.geolocation.Distance;
import fr.cs.regards.geolocation.GeolocationCriteria;
import fr.cs.regards.geolocation.GeolocationService;
import fr.cs.regards.geolocation.Polygon;
import fr.cs.regards.geolocation.Shape;
import fr.cs.regards.geolocation.elastic.binding.ElasticResponse;
import fr.cs.regards.geolocation.elastic.binding.Hit;

public class ElasticGeolacationService implements GeolocationService {
	private static final RestTemplate REST = new RestTemplate();
	private static final String URL_TYPE_TPL = "http://localhost:9200/%s/%s/";
	private static final String SEARCH_TPL = " {\"query\" : {\"match_all\": {}},\"filter\" : {\"geo_shape\":{\"geometry\":{\"shape\":{%s}}}}}";
	private URL url;
	private String index;
	private String type;
	
	
	public ElasticGeolacationService(String url){
		try {
			this.url = new URL(url);
			String[] tokens = this.url.getPath().split("/");
			index = tokens[0];
			type = tokens[1];
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
		
	}
	



	@Override
	public Collection<Feature> select(GeolocationCriteria<? extends Shape> criteria)  throws Exception{
		
		// Set the Content-Type header
		HttpEntity<String> requestEntity = createHTTPRequest(criteria);



		// Add the Jackson and String message converters
		REST.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		REST.getMessageConverters().add(new StringHttpMessageConverter());
		ResponseEntity<ElasticResponse> resp = REST.exchange(url.toString(), HttpMethod.POST, requestEntity, ElasticResponse.class);
		System.out.println(new ObjectMapper().writeValueAsString(resp.getBody()));
		List<Hit> hits = resp.getBody().hits.hits;
		List<Feature> features = new ArrayList<Feature>(hits.size());
		hits.forEach(hit -> {
			features.add(hit._source);
		});
		return features;
		
	}



	private HttpEntity<String> createHTTPRequest(GeolocationCriteria<? extends Shape> criteria) {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(new MediaType("application", "json"));
		
		//Create the query
		String jsonCriteria = ElasticCriteria.convert(criteria).toJSON();
		String query = String.format(SEARCH_TPL, jsonCriteria);
		System.out.println(query);
		HttpEntity<String> requestEntity = new HttpEntity<String>(query, requestHeaders);
		return requestEntity;
	}

	
	public static void main(String[] args) throws MalformedURLException  {
		ElasticGeolacationService service = new ElasticGeolacationService("http://localhost:9200/test/_search/");
		Circle shape = new Circle(new Coordinate(), new Distance(1, Distance.Unit.km));
		GeolocationCriteria<Circle> circle = new GeolocationCriteria<>(shape);
		try {
			service.select(circle);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Polygon poly = new Polygon();
		List<Coordinate> coordinates = poly.getCoordinates();
		coordinates.add(new Coordinate(0.0f, 0.0f));
		coordinates.add(new Coordinate(0.0f, 90.0f));
		coordinates.add(new Coordinate(180.0f, 90.0f));
		coordinates.add(new Coordinate(180.0f, 0.0f));
		GeolocationCriteria<Polygon> polygon = new GeolocationCriteria<>(poly);
		try {
			service.select(polygon);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	@Override
	public void insertFeature(Feature feature) throws Exception {
		String url = String.format(URL_TYPE_TPL, index, type);
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(new MediaType("application", "json"));
		HttpEntity<Feature> requestEntity = new HttpEntity<Feature>(feature, requestHeaders);
		
		REST.exchange(url, HttpMethod.POST, requestEntity, String.class);
		
	}


}
