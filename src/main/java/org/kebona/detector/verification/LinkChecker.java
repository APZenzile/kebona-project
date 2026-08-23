package org.kebona.detector.verification;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.kebona.detector.model.VerificationResult.LinkStatus;

/**
 * The deliberately live, non-reproducible, "right now" check in an otherwise
 * offline/deterministic pipeline. Given a declared IRI, checks whether the
 * resource is still reachable online -- i.e. can an ontology reusing this
 * one actually still rely on the link resolving.
 *
 * Distinguishes two very different failure modes, since collapsing them
 * would corrupt the research data:
 * - a real HTTP response with an error status (404, 410, 5xx, ...) means
 * the server answered and confirmed the resource is gone -> BROKEN
 * - a connection-level failure (DNS failure, timeout, refused connection,
 * no route -- anything before an HTTP response exists at all) means we
 * genuinely don't know whether the resource is broken or just
 * unreachable from here right now -> NOT_CHECKED, not BROKEN
 */
public class LinkChecker {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public LinkChecker() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public LinkStatus check(String declaredIri) {
        if (declaredIri == null || declaredIri.isBlank()) {
            return LinkStatus.NOT_CHECKED;
        }

        URI uri;
        try {
            uri = URI.create(declaredIri);
        } catch (IllegalArgumentException e) {
            return LinkStatus.NOT_CHECKED;
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(TIMEOUT)
                    .build();
        } catch (IllegalArgumentException e) {
            return LinkStatus.NOT_CHECKED;
        }

        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();

            if (status >= 200 && status < 400)
                return LinkStatus.OK;
            else if (status == 401 || status == 403)
                return LinkStatus.RESTRICTED;
            return LinkStatus.BROKEN;

        } catch (IOException e) {
            return LinkStatus.NOT_CHECKED;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return LinkStatus.NOT_CHECKED;
        }
    }
}