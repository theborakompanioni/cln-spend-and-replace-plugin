package org.tbk.cln.snr.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public final class OutputHelper {
    private static final ObjectMapper mapper = new ObjectMapper();

    private OutputHelper() {
        throw new UnsupportedOperationException();
    }

    public static boolean containsObjectWithId(@NonNull ByteArrayOutputStream baos, @NonNull String id) {
        return findObjectWithId(baos, id).isPresent();
    }

    public static Optional<JsonNode> findObjectWithId(@NonNull ByteArrayOutputStream baos, @NonNull String id) {
        String rawOutput = baos.toString(StandardCharsets.UTF_8);
        if (rawOutput.isEmpty()) {
            return Optional.empty();
        }

        return Arrays.stream(rawOutput.replace("}{", "}}{{").split("}\\{"))
                .map(it -> {
                    try {
                        return mapper.readTree(it);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .filter(it -> it.hasNonNull("id"))
                .filter(it -> id.equals(it.get("id").asText()))
                .findFirst();
    }
}
