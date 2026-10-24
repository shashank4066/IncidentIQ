package dev.incidentiq.api;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="app.seed=false")
class SecurityApiTest {
 @Autowired TestRestTemplate http;
 @Test void anonymousCannotReadIncidents(){assertThat(http.getForEntity("/api/incidents",String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);}
 @Test void viewerCanReadButCannotMutate(){var viewer=http.withBasicAuth("viewer","demo-viewer");assertThat(viewer.getForEntity("/api/incidents",String.class).getStatusCode()).isEqualTo(HttpStatus.OK);assertThat(viewer.postForEntity("/api/services",Map.of("slug","new-svc","name","New","team","team"),String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);}
 @Test void responderCannotUseInternalWorkerEndpoint(){assertThat(http.withBasicAuth("admin","demo-admin").postForEntity("/api/internal/incidents/"+UUID.randomUUID()+"/rca",Map.of(),String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);}
 @Test void invalidAlertReturns400(){assertThat(http.withBasicAuth("admin","demo-admin").postForEntity("/api/alerts",Map.of("title","Missing required fields"),String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);}
 @Test void healthDoesNotRequireCredentials(){assertThat(http.getForEntity("/actuator/health/readiness",String.class).getStatusCode()).isEqualTo(HttpStatus.OK);}
}
