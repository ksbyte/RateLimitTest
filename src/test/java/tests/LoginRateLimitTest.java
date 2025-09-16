package tests;


import org.testng.annotations.Test;
import utils.ExcelUtils;
import utils.HttpClientUtil;

//Import
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LoginRateLimitTest {


    // Update these to match your environment
    private static final String EXCEL_PATH = "src/test/resources/data/credentials.xlsx"; // relative to project root
    private static final String SHEET_NAME = null; // null -> first sheet


    // Login endpoint URL
    private static final String LOGIN_URL = "https://payrollapi.hostbooks.in/payroll/login";


    // How many hits we want to send in the burst (you requested 6)
    private static final int HITS_PER_SECOND = 6;


    private final HttpClientUtil http = new HttpClientUtil();


    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());


    @Test
    public void burstPerSecond_loginRateLimit() throws Exception {
        List<ExcelUtils.Credential> creds = ExcelUtils.readCredentials(EXCEL_PATH, SHEET_NAME);
        if (creds.isEmpty()) {
            System.out.println("No credentials found in " + EXCEL_PATH);
            return;
        }


        System.out.println("Read " + creds.size() + " credentials. Sending " + HITS_PER_SECOND + " concurrent requests per credential (burst).");


        for (ExcelUtils.Credential c : creds) {
            System.out.println("\n--- Testing credential: " + c + " at " + TS.format(Instant.now()));


// Build JSON body for login request - adapt field names to your API
            String json = buildLoginJson(c);


// send HITS_PER_SECOND concurrent requests quickly
            List<CompletableFuture<HttpResponse<String>>> futures =
                    java.util.stream.IntStream.range(0, HITS_PER_SECOND)
                            .mapToObj(i -> {
                                Instant start = Instant.now();
                                CompletableFuture<HttpResponse<String>> f = http.postJsonAsync(LOGIN_URL, json)
                                        .whenComplete((resp, t) -> {
                                            String time = TS.format(Instant.now());
                                            if (t != null) {
                                                System.out.println(time + " - Request#" + i + " -> ERROR: " + t.getMessage());
                                            } else {
                                                String body = resp.body();
                                                String snippet = (body == null) ? "" : (body.length() > 200 ? body.substring(0, 200) + "..." : body);
                                                System.out.println(time + " - Request#" + i + " -> status=" + resp.statusCode() + " timeTaken(ms) approx=" + (Instant.now().toEpochMilli() - start.toEpochMilli()) + " bodySnippet=" + snippet);
                                            }
                                        });
                                return f;
                            })
                            .collect(Collectors.toList());


// Wait for all responses but don't block forever
            CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            try {
                all.get(15, TimeUnit.SECONDS);
            } catch (Exception ex) {
                System.out.println("Timed out waiting for responses or error: " + ex.getMessage());
            }


// Optional small pause between credential bursts so you can observe server behavior across creds
            Thread.sleep(500);
        }


        System.out.println("Done.");
    }

    private String buildLoginJson(ExcelUtils.Credential c) {
// Change the JSON field names to match your API contract
// Example: { "tenantId":"...", "email":"...", "password":"..." }
        String t = c.tenantId == null ? "" : c.tenantId.replace("\"", "\\\"");
        String e = c.email == null ? "" : c.email.replace("\"", "\\\"");
        String p = c.password == null ? "" : c.password.replace("\"", "\\\"");
        return String.format("{\"tenantId\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}", t, e, p);
    }
}