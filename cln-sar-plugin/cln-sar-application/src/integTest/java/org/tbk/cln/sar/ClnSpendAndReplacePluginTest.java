package org.tbk.cln.sar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@ActiveProfiles("test")
class ClnSpendAndReplacePluginTest {
    private static final PrintStream standardOut = System.out;
    private static final InputStream standardIn = System.in;

    private static final ByteArrayOutputStream outCaptor = new ByteArrayOutputStream();
    private static final PipedOutputStream inWriter = new PipedOutputStream();
    private static final PipedInputStream inCaptor = new PipedInputStream();

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void beforeAll() throws IOException {
        ClnSpendAndReplacePluginTest.inCaptor.connect(inWriter);
        System.setIn(inCaptor);
        System.setOut(new PrintStream(outCaptor));
    }

    @AfterAll
    static void afterAll() throws IOException {
        System.setOut(ClnSpendAndReplacePluginTest.standardOut);
        System.setIn(ClnSpendAndReplacePluginTest.standardIn);

        outCaptor.close();
        inCaptor.close();
    }

    @Test
    void testGetManifest() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "getmanifest",
                    "params": {}
                }\n
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> outCaptor.size() > 0);

        String output = outCaptor.toString(StandardCharsets.UTF_8);
        assertThat(output, is(notNullValue()));

        JsonNode manifest = mapper.readTree(output).get("result");
        assertThat(manifest.isObject(), is(true));
        assertThat(manifest.get("options").isArray(), is(true));
        assertThat(manifest.get("rpcmethods").isArray(), is(true));
        assertThat(manifest.get("subscriptions").isArray(), is(true));
        assertThat(manifest.get("hooks").isArray(), is(true));
    }

    /*@Test
    void testSnrVersion() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "sar-version",
                    "params": {}
                }\n
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> outCaptor.size() > 0);

        String output = outCaptor.toString(StandardCharsets.UTF_8);
        assertThat(output, is(notNullValue()));

        // TODO: result is a log output, capture the actual response of the method invocation
        JsonNode result = mapper.readTree(output);
        assertThat(result.isObject(), is(true));
    }*/
}
