package org.tbk.cln.sar;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jrpc.clightning.annotation.PluginOption;
import jrpc.clightning.annotation.RPCMethod;
import jrpc.clightning.annotation.Subscription;
import jrpc.clightning.plugins.CLightningPlugin;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.clightning.plugins.log.PluginLog;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ClnSpendAndReplacePlugin extends CLightningPlugin {

    private static final String DEFAULT_FIAT_CURRENCY = "USD";

    @NonNull
    private final ApplicationShutdownManager shutdownManager;

    @NonNull
    private final Exchange exchange;

    @PluginOption(
            name = "sar-dry-run",
            typeValue = "flag",
            defValue = "false",
            description = "Enable dry run. No trades are executed.")
    boolean dryRun;

    @PluginOption(
            name = "sar-default-fiat-currency",
            typeValue = "string",
            defValue = DEFAULT_FIAT_CURRENCY,
            description = "The default fiat currency")
    String defaultFiatCurrency = DEFAULT_FIAT_CURRENCY;

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void onInit(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        super.onInit(plugin, request, response);
        this.log(PluginLog.DEBUG, "spend-and-replace initialized. Request:" + request);
        // test disable (hint: works!)
        // DEBUG   plugin-spend-and-replace: Killing plugin: disabled itself at init: just testing if disabling works
        // response.add("disable", "just testing if disabling works");
    }

    @RPCMethod(
            name = "sar-listconfigs",
            description = "Command to list all configuration options."
    )
    public void rpcListconfigs(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        response.addProperty("dry-run", dryRun);
        response.addProperty("default-fiat-currency", defaultFiatCurrency);
    }

    @RPCMethod(
            name = "sar-version",
            description = "Command to print the plugin version."
    )
    public void rpcVersion(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc version invoked: " + request.getWrapper());

        response.add("version", Optional.of(this.getClass().getPackage())
                .map(Package::getImplementationVersion)
                .orElse("local"));
    }

    @RPCMethod(
            name = "sar-ticker",
            description = "Get the ticker representing the current exchange rate for the provided currency.",
            parameter = "[fiat-currency]"

    )
    public void rpcTicker(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc ticker invoked: " + request.getWrapper());

        Currency bitcoinCurrency = Currency.BTC;
        String fiatCurrenyParam = Optional.ofNullable(request.getAsJsonArray("params"))
                .filter(JsonElement::isJsonArray)
                .filter(it -> !it.isEmpty())
                .map(it -> it.get(0))
                .map(it -> it.getAsJsonPrimitive().getAsString())
                .orElse(this.defaultFiatCurrency);

        Currency fiatCurrency = Currency.getInstance(fiatCurrenyParam);
        CurrencyPair currencyPair = new CurrencyPair(bitcoinCurrency, fiatCurrency);

        boolean supportedCurrencyPair = exchange.getExchangeInstruments().contains(currencyPair);
        if (!supportedCurrencyPair) {
            response.add("error", "Currency pair is not supported: " + currencyPair);
            return;
        }

        try {
            CurrencyPairsParam currencyPairsParam = () -> Collections.singletonList(currencyPair);
            List<Ticker> tickers = exchange.getMarketDataService().getTickers(currencyPairsParam).stream()
                    .filter(it -> currencyPair.equals(it.getInstrument()))
                    .toList();

            JsonObject result = new JsonObject();
            tickers.forEach(ticker -> {
                JsonObject data = new JsonObject();
                data.addProperty("ask", ticker.getAsk().toPlainString());
                data.addProperty("bid", ticker.getBid().toPlainString());
                data.addProperty("high", ticker.getHigh().toPlainString());
                data.addProperty("low", ticker.getLow().toPlainString());
                data.addProperty("open", ticker.getOpen().toPlainString());
                data.addProperty("last", ticker.getLast().toPlainString());

                result.add(ticker.getInstrument().toString(), data);
            });

            response.add("result", result);
        } catch (IOException e) {
            response.add("error", e.getMessage());
        }
    }

    @Subscription(notification = "shutdown")
    public void shutdown(CLightningJsonObject data) {
        System.exit(shutdownManager.initiateShutdown(0));
    }

    // https://lightning.readthedocs.io/PLUGINS.html?#sendpay-success
    @Subscription(notification = "sendpay_success")
    public void onNotificationSendpaySuccess(CLightningJsonObject data) {
        log(PluginLog.DEBUG, "Notification sendpay_success received.");

        try {
            long amountWithFees = data.getAsJsonObject("sendpay_success")
                    .getAsJsonPrimitive("amount_sent_msat")
                    .getAsLong();
            log(PluginLog.DEBUG, "Spent amount which needs to be replaced: " + amountWithFees);

            if (dryRun) {
                log(PluginLog.INFO, "Dry run is active; would have replaced: " + amountWithFees);
            } else {
                log(PluginLog.ERROR, "Error while replacing amount: Not implemented yet");
            }
        } catch (Exception e) {
            // empty on purpose
        }
    }
}
