package org.tbk.cln.snr;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.tbk.cln.snr.test.OutputHelper.containsObjectWithId;
import static org.tbk.cln.snr.test.OutputHelper.findObjectWithId;
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
    public void setUp() throws IOException {
        outCaptor.reset();

        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "init",
                    "method": "init",
                    "params": {
                      "options": {
                        "snr-dry-run": true,
                        "snr-default-fiat-currency": "USD"
                      },
                      "configuration":{
                        "lightning-dir": "/home/clightning/.lightning/regtest",
                        "rpc-file": "lightning-rpc",
                        "startup": true,
                        "network": "regtest",
                        "feature_set": {
                          "init": "08a000080269a2",
                          "node": "88a000080269a2",
                          "channel": "",
                          "invoice":"02000000024100"
                        }
                      }
                    }
                }
                """.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testGetManifest() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "getmanifest",
                    "method": "getmanifest",
                    "params": []
                }
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "getmanifest"));

        JsonNode output = findObjectWithId(outCaptor, "getmanifest").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.toPrettyString(), is("""
                {
                  "options" : [ {
                    "name" : "snr-dry-run",
                    "type" : "flag",
                    "default" : "false",
                    "description" : "Enable dry run. Trades are executed against a demo exchange."
                  }, {
                    "name" : "snr-default-fiat-currency",
                    "type" : "string",
                    "default" : "USD",
                    "description" : "The default fiat currency"
                  } ],
                  "rpcmethods" : [ {
                    "name" : "snr-listconfigs",
                    "usage" : "",
                    "description" : "Command to list all configuration options."
                  }, {
                    "name" : "snr-history",
                    "usage" : "",
                    "description" : "Get the trade history of your account."
                  }, {
                    "name" : "snr-placetestorder",
                    "usage" : "",
                    "description" : "Place a minimal, greatly undervalued limit order to test if exchange settings are working properly."
                  }, {
                    "name" : "snr-balance",
                    "usage" : "",
                    "description" : "Get the balance of your account."
                  }, {
                    "name" : "snr-version",
                    "usage" : "",
                    "description" : "Command to print the plugin version."
                  }, {
                    "name" : "snr-exchangeinfo",
                    "usage" : "",
                    "description" : "Command to list exchange specific information."
                  }, {
                    "name" : "snr-ticker",
                    "usage" : "[fiat-currency]",
                    "description" : "Get the ticker representing the current exchange rate for the provided currency."
                  } ],
                  "subscriptions" : [ "shutdown", "sendpay_success" ],
                  "hooks" : [ ],
                  "features" : {
                    "node" : null,
                    "channel" : null,
                    "init" : null,
                    "invoice" : null
                  },
                  "dynamic" : true
                }"""));
    }

    @Test
    void testSnrListconfigs() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "snr-listconfigs",
                    "method": "snr-listconfigs",
                    "params": []
                }
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "snr-listconfigs"));

        JsonNode output = findObjectWithId(outCaptor, "snr-listconfigs").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.toPrettyString(), is("""
                {
                  "result" : {
                    "dry-run" : true,
                    "fiat-currency" : {
                      "default" : "USD"
                    },
                    "exchange" : {
                      "name" : "Dummy",
                      "host" : "localhost:8883"
                    }
                  }
                }"""));
    }

    @Test
    void testSnrVersion() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "snr-version",
                    "method": "snr-version",
                    "params": []
                }
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "snr-version"));

        JsonNode output = findObjectWithId(outCaptor, "snr-version").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.isObject(), is(true));

        JsonNode versionResult = result.get("result");

        // regex taken directly from https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
        assertThat(versionResult.get("version").asText("-"), matchesRegex("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$"));
    }

    @Test
    void testSnrExchangeInfo() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "snr-exchangeinfo",
                    "method": "snr-exchangeinfo",
                    "params": []
                }
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "snr-exchangeinfo"));

        JsonNode output = findObjectWithId(outCaptor, "snr-exchangeinfo").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.toPrettyString(), is("""
                {
                  "result" : {
                    "name" : "Dummy",
                    "description" : "Dummy is an exchange that should only be used while testing",
                    "host" : "localhost:8883",
                    "metadata" : {
                      "instruments" : {
                        "BTC/USD" : {
                          "min-amount" : "0.00001"
                        }
                      }
                    }
                  }
                }"""));
    }

    @Test
    void testSnrTicker() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "snr-ticker",
                    "method": "snr-ticker",
                    "params": []
                }
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(50))
                .until(() -> containsObjectWithId(outCaptor, "snr-ticker"));

        JsonNode output = findObjectWithId(outCaptor, "snr-ticker").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.toPrettyString(), is("""
                {
                  "result" : {
                    "BTC/USD" : {
                      "ask" : "0.12",
                      "bid" : "0.14",
                      "high" : "0.15",
                      "low" : "0.17",
                      "open" : "0.18",
                      "last" : "0.16"
                    }
                  }
                }"""));
    }

    @Test
    void testSnrTickerWithParam() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "snr-ticker-gbp",
                    "method": "snr-ticker",
                    "params": ['GBP']
                }
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "snr-ticker-gbp"));

        JsonNode output = findObjectWithId(outCaptor, "snr-ticker-gbp").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.toPrettyString(), is("""
                {
                  "result" : {
                    "BTC/GBP" : {
                      "ask" : "0.12",
                      "bid" : "0.14",
                      "high" : "0.15",
                      "low" : "0.17",
                      "open" : "0.18",
                      "last" : "0.16"
                    }
                  }
                }"""));
    }


    @Test
    void testSnrBalance() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "snr-balance",
                    "method": "snr-balance",
                    "params": []
                }
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> containsObjectWithId(outCaptor, "snr-balance"));

        JsonNode output = findObjectWithId(outCaptor, "snr-balance").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.toPrettyString(), is("""
                {
                  "result" : {
                    "_" : {
                      "id" : null,
                      "name" : null,
                      "balances" : {
                        "BTC" : {
                          "available" : "0",
                          "available-for-withdrawal" : "0",
                          "borrowed" : "0",
                          "depositing" : "0",
                          "frozen" : "0",
                          "loaned" : "0",
                          "total" : "0.0000000001",
                          "withdrawing" : "0"
                        },
                        "USD" : {
                          "available" : "0",
                          "available-for-withdrawal" : "0",
                          "borrowed" : "0",
                          "depositing" : "0",
                          "frozen" : "0",
                          "loaned" : "0",
                          "total" : "0.0001",
                          "withdrawing" : "0"
                        }
                      }
                    },
                    "margin" : {
                      "id" : "margin",
                      "name" : "margin",
                      "balances" : {
                        "BTC" : {
                          "available" : "0",
                          "available-for-withdrawal" : "0",
                          "borrowed" : "0",
                          "depositing" : "0",
                          "frozen" : "0",
                          "loaned" : "0",
                          "total" : "0.0000000001",
                          "withdrawing" : "0"
                        },
                        "USD" : {
                          "available" : "0",
                          "available-for-withdrawal" : "0",
                          "borrowed" : "0",
                          "depositing" : "0",
                          "frozen" : "0",
                          "loaned" : "0",
                          "total" : "0.0001",
                          "withdrawing" : "0"
                        }
                      }
                    }
                  }
                }"""));
    }

    @Test
    void testSnrHistory() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "snr-history",
                    "method": "snr-history",
                    "params": []
                }
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(555))
                .until(() -> containsObjectWithId(outCaptor, "snr-history"));

        JsonNode output = findObjectWithId(outCaptor, "snr-history").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.toPrettyString(), is("""
                {
                  "result" : {
                    "open" : {
                      "abcdef-00000-000001" : {
                        "id" : "abcdef-00000-000001",
                        "type" : "BID",
                        "status" : "NEW",
                        "is-open" : true,
                        "is-final" : false,
                        "original-amount" : "0.42",
                        "remaining-amount" : "0.42",
                        "limit-price" : "21.0",
                        "asset-pair" : "BTC/USD",
                        "ref" : "0",
                        "date" : "2021-05-26T03:33:20Z",
                        "timestamp" : 1622000000
                      }
                    },
                    "closed" : {
                      "abcdef-00000-000000" : {
                        "id" : "abcdef-00000-000000",
                        "type" : "BID",
                        "order-id" : "abcdef",
                        "price" : "21000.0",
                        "original-amount" : "0.21",
                        "asset-pair" : "BTC/USD",
                        "ref" : "",
                        "fee-amount" : "0.090103",
                        "fee-currency" : "USD",
                        "date" : "2021-05-14T13:46:40Z",
                        "timestamp" : 1621000000
                      }
                    }
                  }
                }"""));
    }

    @Test
    void testSnrPlaceTestOrder() throws IOException {
        inWriter.write("""
                {
                    "jsonrpc": "2.0",
                    "id": "snr-placetestorder",
                    "method": "snr-placetestorder",
                    "params": []
                }
                """.getBytes(StandardCharsets.UTF_8));

        await()
                .atMost(Duration.ofSeconds(1115))
                .until(() -> containsObjectWithId(outCaptor, "snr-placetestorder"));

        JsonNode output = findObjectWithId(outCaptor, "snr-placetestorder").orElseThrow();

        JsonNode result = output.get("result");
        assertThat(result.toPrettyString(), is("""
                {
                  "result" : {
                    "order" : {
                      "id" : "1",
                      "type" : "BID",
                      "asset-pair" : "BTC/USD",
                      "amount" : "0.00001000",
                      "price" : "0.02"
                    }
                  }
                }"""));
    }
}
