/**
 * Copyright (C) Alejandro Ayuso
 *
 * This file is part of Amforeas. Amforeas is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Amforeas is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Amforeas. If not, see <http://www.gnu.org/licenses/>.
 */

package amforeas.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import amforeas.client.handler.AmforeasResponseHandler;
import amforeas.client.model.AmforeasResponse;
import amforeas.client.model.ErrorResponse;
import amforeas.client.model.StoredProcedureParam;

public class AmforeasRestClient implements AmforeasClient<AmforeasResponse> {

    private static final Logger l = LoggerFactory.getLogger(AmforeasRestClient.class);

    private static final String alias_path = "%s/%s";
    private static final String resource_path = "%s/%s/%s";
    private static final String item_path = "%s/%s/%s/%s";
    private static final String find_path = "%s/%s/%s/%s/%s";
    private static final String query_path = "%s/%s/%s/dynamic/%s";
    private static final String call_path = "%s/%s/call/%s";

    private final String protocol;
    private final String host;
    private final Integer port;
    private final String root;
    private final String alias;
    private final Header accept;

    public AmforeasRestClient(String protocol, String host, Integer port, String root, String alias) {
        validateInput(protocol, host, port, root, alias);

        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.root = root;
        this.alias = alias;
        this.accept = new BasicHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
    }

    public AmforeasRestClient(String protocol, String host, Integer port, String root, String alias, String format) {
        validateInput(protocol, host, port, root, alias);

        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.root = root;
        this.alias = alias;
        this.accept = new BasicHeader(HttpHeaders.ACCEPT, format);
    }

    private void validateInput (String protocol, String host, Integer port, String root, String alias) {
        if (StringUtils.isEmpty(protocol) || !(protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Invalid protocol value: " + protocol);
        }

        if (StringUtils.isEmpty(host)) {
            throw new IllegalArgumentException("Invalid host value: " + host);
        }

        if (StringUtils.isEmpty(root)) {
            throw new IllegalArgumentException("Invalid root value: " + root);
        }

        if (StringUtils.isEmpty(alias)) {
            throw new IllegalArgumentException("Invalid alias value: " + alias);
        }
    }

    /* Meta */

    public Optional<AmforeasResponse> meta () {
        final URI url = this.build(String.format(alias_path, root, alias)).orElseThrow();
        final HttpGet req = new HttpGet(url);
        req.addHeader(this.accept);
        return this.execute(req);
    }

    public Integer meta (String resource) {
        final URI url = this.build(String.format(resource_path, root, alias, resource)).orElseThrow();
        final HttpHead req = new HttpHead(url);
        req.addHeader(this.accept);
        return this.execute(req);
    }

    /* READ */

    public Optional<AmforeasResponse> getAll (String resource) {
        final URI url = this.build(String.format(resource_path, root, alias, resource)).orElseThrow();
        final HttpGet req = new HttpGet(url);
        req.addHeader(this.accept);
        return this.execute(req);
    }

    public Optional<AmforeasResponse> get (String resource, String id) {
        return this.get(resource, "id", id);
    }

    public Optional<AmforeasResponse> get (String resource, String pk, String id) {
        final URI url = this.build(String.format(item_path, root, alias, resource, id)).orElseThrow();
        final HttpGet req = new HttpGet(url);
        req.addHeader(this.accept);
        req.addHeader("Primary-Key", pk);
        return this.execute(req);
    }

    public Optional<AmforeasResponse> find (String resource, String col, String arg) {
        final URI url = this.build(String.format(find_path, root, alias, resource, col, arg)).orElseThrow();
        final HttpGet req = new HttpGet(url);
        req.addHeader(this.accept);
        return this.execute(req);
    }

    public Optional<AmforeasResponse> query (String resource, String query, String... args) {
        final NameValuePair[] nvps = Arrays.asList(args)
            .stream()
            .map(obj -> new BasicNameValuePair("args", obj))
            .collect(Collectors.toList())
            .toArray(new NameValuePair[] {});

        final URI url = this.build(String.format(query_path, root, alias, resource, query), nvps).orElseThrow();
        final HttpGet req = new HttpGet(url);
        return this.execute(req);
    }

    /* CREATE */

    public Optional<AmforeasResponse> add (String resource, String json) {
        final URI url = this.build(String.format(resource_path, root, alias, resource)).orElseThrow();
        final HttpPost req = new HttpPost(url);
        req.addHeader(this.accept);
        req.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try {
            req.setEntity(new StringEntity(json));
        } catch (UnsupportedEncodingException e) {
            final String msg = "Failed to encode JSON body " + e.getMessage();
            l.error(msg);
            return Optional.of(new ErrorResponse(resource, Response.Status.BAD_REQUEST, msg));
        }

        return this.execute(req);
    }

    public Optional<AmforeasResponse> add (String resource, List<NameValuePair> params) {
        final URI url = this.build(String.format(resource_path, root, alias, resource)).orElseThrow();
        final HttpPost req = new HttpPost(url);
        req.addHeader(this.accept);
        req.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

        try {
            req.setEntity(new UrlEncodedFormEntity(params));
        } catch (UnsupportedEncodingException e) {
            final String msg = "Failed to encode form body " + e.getMessage();
            l.error(msg);
            return Optional.of(new ErrorResponse(resource, Response.Status.BAD_REQUEST, msg));
        }

        return this.execute(req);
    }

    /* UPDATE */

    public Optional<AmforeasResponse> update (String resource, String id, String json) {
        return this.update(resource, "id", id, json);
    }

    public Optional<AmforeasResponse> update (String resource, String pk, String id, String json) {
        final URI url = this.build(String.format(item_path, root, alias, resource, id)).orElseThrow();
        final HttpPut req = new HttpPut(url);
        req.addHeader(this.accept);
        req.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        req.addHeader("Primary-Key", pk);

        try {
            req.setEntity(new StringEntity(json));
        } catch (UnsupportedEncodingException e) {
            final String msg = "Failed to encode JSON body " + e.getMessage();
            l.error(msg);
            return Optional.of(new ErrorResponse(resource, Response.Status.BAD_REQUEST, msg));
        }

        return this.execute(req);
    }

    /* DELETE */

    public Optional<AmforeasResponse> delete (String resource, String id) {
        return this.delete(resource, "id", id);
    }

    public Optional<AmforeasResponse> delete (String resource, String pk, String id) {
        final URI url = this.build(String.format(item_path, root, alias, resource, id)).orElseThrow();
        final HttpDelete req = new HttpDelete(url);
        req.addHeader(this.accept);
        req.addHeader("Primary-Key", pk);
        return this.execute(req);
    }

    /* Functions and Stored Procedure */

    public Optional<AmforeasResponse> call (String function, StoredProcedureParam... params) {
        final URI url = this.build(String.format(call_path, root, alias, function)).orElseThrow();
        final HttpPost req = new HttpPost(url);
        req.addHeader(this.accept);
        req.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

        try {
            req.setEntity(new StringEntity(writeAsJSON(params)));
        } catch (Exception e) {
            final String msg = "Failed to encode JSON body " + e.getMessage();
            l.error(msg);
            return Optional.of(new ErrorResponse(alias, Response.Status.BAD_REQUEST, msg));
        }

        return this.execute(req);
    }

    /* With Request Params */

    public Optional<AmforeasResponse> get (final RequestParams request) {
        final URI url = this.build(request).orElseThrow();
        final HttpGet req = new HttpGet(url);
        req.addHeader(this.accept);

        if (StringUtils.isNotEmpty(request.getPrimaryKey())) {
            req.addHeader("Primary-Key", request.getPrimaryKey());
        }

        return this.execute(req);
    }

    public Optional<AmforeasResponse> post (final RequestParams request, String contentType) {
        final URI url = this.build(request).orElseThrow();
        final HttpPost req = new HttpPost(url);
        req.addHeader(this.accept);
        req.addHeader(HttpHeaders.CONTENT_TYPE, contentType);

        try {
            req.setEntity(contentType == MediaType.APPLICATION_JSON ? request.getJSONBody() : request.getFormBody());
        } catch (Exception e) {
            final String msg = "Failed to encode body " + e.getMessage();
            l.error(msg);
            return Optional.of(new ErrorResponse(request.getResource(), Response.Status.BAD_REQUEST, msg));
        }

        return this.execute(req);
    }

    public Optional<AmforeasResponse> put (final RequestParams request, String contentType) {
        final URI url = this.build(request).orElseThrow();
        final HttpPut req = new HttpPut(url);
        req.addHeader(this.accept);
        req.addHeader(HttpHeaders.CONTENT_TYPE, contentType);
        req.addHeader("Primary-Key", request.getPrimaryKey());

        try {
            req.setEntity(contentType == MediaType.APPLICATION_JSON ? request.getJSONBody() : request.getFormBody());
        } catch (Exception e) {
            final String msg = "Failed to encode body " + e.getMessage();
            l.error(msg);
            return Optional.of(new ErrorResponse(request.getResource(), Response.Status.BAD_REQUEST, msg));
        }

        return this.execute(req);
    }

    public Optional<AmforeasResponse> delete (final RequestParams request) {
        final URI url = this.build(request).orElseThrow();
        final HttpDelete req = new HttpDelete(url);
        req.addHeader(this.accept);
        req.addHeader("Primary-Key", request.getPrimaryKey());
        return this.execute(req);
    }

    private Optional<URI> build (String path, NameValuePair... nvps) {
        URI url = null;
        try {
            url = new URIBuilder()
                .setScheme(this.protocol)
                .setHost(this.host)
                .setPort(this.port)
                .setPath(path)
                .setParameters(nvps)
                .build();
        } catch (URISyntaxException e) {
            l.error("Failed to build URL: {}", e.getMessage());
        }

        this.logRequest(url);
        return Optional.ofNullable(url);
    }

    private Optional<URI> build (final RequestParams request) {
        URI url = null;
        try {
            url = new URIBuilder()
                .setScheme(this.protocol)
                .setHost(this.host)
                .setPort(this.port)
                .setPath(request.getPath(this.root, this.alias))
                .setParameters(request.getParameters())
                .build();
        } catch (URISyntaxException e) {
            l.error("Failed to build URL: {}", e.getMessage());
        }

        this.logRequest(url);
        return Optional.ofNullable(url);
    }

    private Optional<AmforeasResponse> execute (HttpUriRequest request) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            return Optional.ofNullable(client.execute(request, new AmforeasResponseHandler()));
        } catch (ClientProtocolException e) {
            l.warn("Invalid protocol: {}", e.getMessage());
        } catch (IOException e) {
            l.warn("Failed to connect: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Integer execute (HttpHead request) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = client.execute(request)) {
                return response.getStatusLine().getStatusCode();
            } catch (Exception ex) {
                l.warn("Failed to connect: {}", ex.getMessage());
            }
        } catch (ClientProtocolException e) {
            l.warn("Invalid protocol: {}", e.getMessage());
        } catch (IOException e) {
            l.warn("Failed to connect: {}", e.getMessage());
        }

        return 400;
    }

    private void logRequest (final URI uri) {
        if (l.isDebugEnabled()) {
            l.debug("Performing request {}", uri.toASCIIString());
        }
    }

    /**
     * Convert an object to JSON
     * @param obj - the object to convert
     * @return a string
     * @throws JsonProcessingException 
     */
    private String writeAsJSON (final Object obj) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(obj);
    }

}
