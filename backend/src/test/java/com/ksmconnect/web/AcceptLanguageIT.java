package com.ksmconnect.web;

import com.ksmconnect.support.TestNotificationConfiguration;
import com.ksmconnect.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestNotificationConfiguration.class})
class AcceptLanguageIT {

    private static final String BROWSER_WITH_MANY_LANGUAGES =
            "pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7,de-DE;q=0.6,de;q=0.5,fr-FR;q=0.4,fr;q=0.3,"
                    + "es-ES;q=0.3,es;q=0.2,it-IT;q=0.2,it;q=0.2,uk-UA;q=0.1,uk;q=0.1,ru;q=0.1";

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void pagesLoadForBrowsersConfiguredWithManyLanguages() throws Exception {
        for (String path : new String[]{"/", "/login", "/api/auth/csrf", "/actuator/health"}) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Accept-Language", BROWSER_WITH_MANY_LANGUAGES)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(path).isLessThan(500);
        }
    }
}
