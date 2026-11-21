package com.inkwell.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inkwell.common.ApiException;
import com.inkwell.config.AppProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Thin HTTP client for the Python ML sidecar (embeddings, handwriting recognition and generation). */
@Component
public class MlClient {
    private static final Logger log = LoggerFactory.getLogger(MlClient.class);

    public record RecognizeResponse(String text, String language, String engine) {}
    public record SheetReport(String sheet, int found, int total, String missing) {}
    public record ExtractResponse(String packB64, String covered, String missing, List<SheetReport> sheets) {}
    public record RenderResponse(String imageB64, int width, int height, String missing) {}
    private record EmbedResponse(List<List<Double>> vectors, int dim) {}
    private record AnswerResponse(String answer) {}

    private final RestClient http;
    private final ObjectMapper mapper;

    public MlClient(AppProperties props, ObjectMapper mapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4_000);
        factory.setReadTimeout((int) props.ml().timeout().toMillis());
        this.http = RestClient.builder().baseUrl(props.ml().baseUrl()).requestFactory(factory).build();
        this.mapper = mapper;
    }

    /** Returns empty (never throws) so search degrades gracefully to full-text when ML is down. */
    public Optional<float[]> embed(String text) {
        try {
            EmbedResponse r = http.post().uri("/embed").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("texts", List.of(text))).retrieve().body(EmbedResponse.class);
            if (r == null || r.vectors() == null || r.vectors().isEmpty()) return Optional.empty();
            List<Double> v = r.vectors().get(0);
            float[] out = new float[v.size()];
            for (int i = 0; i < out.length; i++) out[i] = v.get(i).floatValue();
            return Optional.of(out);
        } catch (Exception e) {
            log.warn("Embedding unavailable: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public RecognizeResponse recognize(byte[] image, String filename) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("image", named(image, filename));
        return call(() -> http.post().uri("/recognize").contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts).retrieve().body(RecognizeResponse.class));
    }

    public ResponseEntity<byte[]> template(String sheets, String format) {
        return call(() -> http.get().uri(u -> u.path("/template").queryParam("sheets", sheets)
                .queryParam("format", format).build()).retrieve().toEntity(byte[].class));
    }

    public ExtractResponse extractGlyphs(List<NamedBytes> sheets) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        sheets.forEach(s -> parts.add("sheets", named(s.data(), s.filename())));
        return call(() -> http.post().uri("/glyphs/extract").contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts).retrieve().body(ExtractResponse.class));
    }

    public RenderResponse render(Map<String, Object> body) {
        return call(() -> http.post().uri("/glyphs/render").contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(RenderResponse.class));
    }

    public String answer(String query, List<Map<String, Object>> sources) {
        AnswerResponse r = call(() -> http.post().uri("/answer").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", query, "sources", sources)).retrieve().body(AnswerResponse.class));
        return r == null ? null : r.answer();
    }

    public record NamedBytes(byte[] data, String filename) {}

    private static ByteArrayResource named(byte[] data, String filename) {
        String name = filename == null || filename.isBlank() ? "upload.bin" : filename;
        return new ByteArrayResource(data) {
            @Override public String getFilename() { return name; }
        };
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            String detail = detail(e.getResponseBodyAsString());
            HttpStatus status = e.getStatusCode().is4xxClientError() ? HttpStatus.UNPROCESSABLE_ENTITY
                    : e.getStatusCode().value() == 503 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
            throw new ApiException(status, detail);
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The ML service is not reachable right now");
        }
    }

    private String detail(String body) {
        try {
            JsonNode n = mapper.readTree(body);
            if (n.hasNonNull("detail")) return n.get("detail").asText();
        } catch (Exception ignored) { /* fall through */ }
        return "The ML service returned an error";
    }
}
