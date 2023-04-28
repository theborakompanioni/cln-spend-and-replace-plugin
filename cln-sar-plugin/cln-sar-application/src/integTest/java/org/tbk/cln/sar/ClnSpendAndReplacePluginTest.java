package org.tbk.cln.sar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.tbk.cln.sar.test.OutputHelper.containsObjectWithId;
import static org.tbk.cln.sar.test.OutputHelper.findObjectWithId;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@ActiveProfiles("test")
class ClnSpendAndReplacePluginTest {
    private static final PrintStream standardOut = System.out;
    private static final InputStream standardIn = System.in;

    private static final ByteArrayOutputStream outCaptor = new ByteArrayOutputStream();
    private static final PipedOutputStream inWriter = new PipedOutputStream();
    private static final PipedInputStream inCaptor = new PipedInputStream();

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

    @BeforeEach
    public void setUp() {
        outCaptor.reset();
    }

    @Test
    void testGetManifest() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "getmanifest",
                    "method": "getmanifest",
                    "params": []
                }\n
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "getmanifest"));

        JsonNode output = findObjectWithId(outCaptor, "getmanifest").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.isObject(), is(true));
        assertThat(result.get("options").isArray(), is(true));
        assertThat(result.get("rpcmethods").isArray(), is(true));
        assertThat(result.get("subscriptions").isArray(), is(true));
        assertThat(result.get("hooks").isArray(), is(true));

        List<String> optionNames = StreamSupport.stream(result.get("options").spliterator(), false)
                .map(it -> it.get("name").asText("-"))
                .toList();

        assertThat(optionNames, hasItems("sar-dry-run", "sar-default-fiat-currency"));

        List<String> rpcMethodNames = StreamSupport.stream(result.get("rpcmethods").spliterator(), false)
                .map(it -> it.get("name").asText("-"))
                .toList();

        assertThat(rpcMethodNames, hasItems("sar-listconfigs", "sar-ticker", "sar-version"));

        List<String> subscriptions = StreamSupport.stream(result.get("subscriptions").spliterator(), false)
                .map(it -> it.asText("-"))
                .toList();

        assertThat(subscriptions, hasItems("shutdown", "sendpay_success"));
    }

    @Test
    void testSarListconfigs() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "sar-listconfigs",
                    "method": "sar-listconfigs",
                    "params": []
                }\n
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "sar-listconfigs"));

        JsonNode output = findObjectWithId(outCaptor, "sar-listconfigs").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.isObject(), is(true));

        JsonNode configResult = result.get("result");
        assertThat(configResult.size(), is(3)); // adapt if you add new values

        assertThat(configResult.get("dry-run").asText("-"), is("false"));
        assertThat(configResult.get("fiat-currency").get("default").asText("-"), is("USD"));
        assertThat(configResult.get("exchange").get("name").asText("-"), is("Dummy"));
        assertThat(configResult.get("exchange").get("host").asText("-"), is(notNullValue()));
    }

    @Test
    void testSarVersion() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "sar-version",
                    "method": "sar-version",
                    "params": []
                }\n
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "sar-version"));

        JsonNode output = findObjectWithId(outCaptor, "sar-version").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.isObject(), is(true));

        JsonNode versionResult = result.get("result");

        assertThat(versionResult.get("version").asText("-"), is("local"));
    }

    @Test
    void testSarTicker() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "sar-ticker",
                    "method": "sar-ticker",
                    "params": []
                }\n
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(50))
                .until(() -> containsObjectWithId(outCaptor, "sar-ticker"));

        JsonNode output = findObjectWithId(outCaptor, "sar-ticker").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.isObject(), is(true));

        JsonNode tickerResult = result.get("result");
        assertThat(tickerResult.hasNonNull("BTC/USD"), is(true));

        JsonNode btcUsdTicker = tickerResult.get("BTC/USD");
        assertThat(btcUsdTicker.size(), is(6));
        assertThat(btcUsdTicker.get("ask").asText("-"), is("0.12"));
        assertThat(btcUsdTicker.get("bid").asText("-"), is("0.14"));
        assertThat(btcUsdTicker.get("high").asText("-"), is("0.15"));
        assertThat(btcUsdTicker.get("low").asText("-"), is("0.17"));
        assertThat(btcUsdTicker.get("open").asText("-"), is("0.18"));
        assertThat(btcUsdTicker.get("last").asText("-"), is("0.16"));
    }

    @Test
    void testSarBalance() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "sar-balance",
                    "method": "sar-balance",
                    "params": []
                }\n
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "sar-balance"));

        JsonNode output = findObjectWithId(outCaptor, "sar-balance").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.isObject(), is(true));

        JsonNode walletResult = result.get("result");
        assertThat(walletResult.isObject(), is(true));
        assertThat(walletResult.size(), is(2));

        JsonNode defaultWallet = walletResult.get("_");
        assertThat(defaultWallet.get("id").asText("-"), is("-"));
        assertThat(defaultWallet.get("name").asText("-"), is("-"));
        assertThat(defaultWallet.get("balances").isObject(), is(true));
        assertThat(defaultWallet.get("balances").get("BTC").isObject(), is(true));
        assertThat(defaultWallet.get("balances").get("BTC").get("total").asText("-"), is("0.0000000001"));
        assertThat(defaultWallet.get("balances").get("USD").isObject(), is(true));
        assertThat(defaultWallet.get("balances").get("USD").get("total").asText("-"), is("0.0001"));

        JsonNode marginWallet = walletResult.get("margin");
        assertThat(marginWallet.get("id").asText("-"), is("margin"));
        assertThat(marginWallet.get("name").asText("-"), is("margin"));
        assertThat(marginWallet.get("balances").isObject(), is(true));
        assertThat(marginWallet.get("balances").get("BTC").isObject(), is(true));
        assertThat(marginWallet.get("balances").get("BTC").get("total").asText("-"), is("0.0000000001"));
        assertThat(marginWallet.get("balances").get("USD").isObject(), is(true));
        assertThat(marginWallet.get("balances").get("USD").get("total").asText("-"), is("0.0001"));
    }

    @Test
    void testSarTickerWithParam() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "sar-ticker-gbp",
                    "method": "sar-ticker",
                    "params": ['GBP']
                }\n
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "sar-ticker-gbp"));

        JsonNode output = findObjectWithId(outCaptor, "sar-ticker-gbp").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.isObject(), is(true));

        JsonNode tickerResult = result.get("result");
        assertThat(tickerResult.hasNonNull("BTC/GBP"), is(true));

        JsonNode btcUsdTicker = tickerResult.get("BTC/GBP");
        assertThat(btcUsdTicker.size(), is(6));
        assertThat(btcUsdTicker.get("ask").asText("-"), is("0.12"));
        assertThat(btcUsdTicker.get("bid").asText("-"), is("0.14"));
        assertThat(btcUsdTicker.get("high").asText("-"), is("0.15"));
        assertThat(btcUsdTicker.get("low").asText("-"), is("0.17"));
        assertThat(btcUsdTicker.get("open").asText("-"), is("0.18"));
        assertThat(btcUsdTicker.get("last").asText("-"), is("0.16"));
    }

}
