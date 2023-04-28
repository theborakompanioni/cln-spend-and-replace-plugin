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
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

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
        log(PluginLog.DEBUG, "rpc 'sar-listconfigs' invoked: " + request.getWrapper());

        response.addProperty("dry-run", dryRun);

        JsonObject fiatCurrencyData = new JsonObject();
        fiatCurrencyData.addProperty("default", defaultFiatCurrency);
        response.add("fiat-currency", fiatCurrencyData);

        JsonObject exchangeData = new JsonObject();
        exchangeData.addProperty("name", exchange.getExchangeSpecification().getExchangeName());
        exchangeData.addProperty("host", exchange.getExchangeSpecification().getHost());
        response.add("exchange", exchangeData);
    }

    @RPCMethod(
            name = "sar-version",
            description = "Command to print the plugin version."
    )
    public void rpcVersion(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc 'sar-version' invoked: " + request.getWrapper());

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
        log(PluginLog.DEBUG, "rpc 'sar-ticker' invoked: " + request.getWrapper());

        Currency bitcoinCurrency = Currency.BTC;
        String fiatCurrencyParam = Optional.ofNullable(request.getAsJsonArray("params"))
                .filter(JsonElement::isJsonArray)
                .filter(it -> !it.isEmpty())
                .map(it -> it.get(0))
                .map(it -> it.getAsJsonPrimitive().getAsString())
                .orElse(this.defaultFiatCurrency);

        Currency fiatCurrency = Currency.getInstance(fiatCurrencyParam);
        CurrencyPair currencyPair = new CurrencyPair(bitcoinCurrency, fiatCurrency);

        initExchangeIfNecessary();

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


    @RPCMethod(
            name = "sar-balance",
            description = "Get the balance of your account."
    )
    public void rpcBalance(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        log(PluginLog.DEBUG, "rpc 'sar-balance' invoked: " + request.getWrapper());

        initExchangeIfNecessary();

        try {
            Map<String, Wallet> wallets = exchange.getAccountService()
                    .getAccountInfo()
                    .getWallets();

            if (wallets.isEmpty()) {
                response.add("error", "There is no wallet available.");
            } else {
                Map<String, JsonObject> walletJsonMap = wallets.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, walletEntry -> {
                            Wallet wallet = walletEntry.getValue();

                            Map<Currency, Balance> positiveBalances = wallet.getBalances().entrySet().stream()
                                    .filter(it -> it.getValue().getTotal().compareTo(BigDecimal.ZERO) > 0)
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                            Map<Currency, JsonObject> currencyBalanceJsonMap = positiveBalances.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                                        Balance balance = entry.getValue();

                                        JsonObject data = new JsonObject();
                                        data.addProperty("available", nullToZero(balance.getAvailable()).toPlainString());
                                        data.addProperty("available-for-withdrawal", nullToZero(balance.getAvailableForWithdrawal()).toPlainString());
                                        data.addProperty("borrowed", nullToZero(balance.getBorrowed()).toPlainString());
                                        data.addProperty("depositing", nullToZero(balance.getDepositing()).toPlainString());
                                        data.addProperty("frozen", nullToZero(balance.getFrozen()).toPlainString());
                                        data.addProperty("loaned", nullToZero(balance.getLoaned()).toPlainString());
                                        data.addProperty("total", nullToZero(balance.getTotal()).toPlainString());
                                        data.addProperty("withdrawing", nullToZero(balance.getWithdrawing()).toPlainString());
                                        return data;
                                    }));

                            JsonObject balancesData = new JsonObject();
                            currencyBalanceJsonMap.forEach((key, val) -> balancesData.add(key.getCurrencyCode(), val));

                            JsonObject walletJson = new JsonObject();
                            walletJson.addProperty("id", wallet.getId());
                            walletJson.addProperty("name", wallet.getName());
                            walletJson.add("balances", balancesData);
                            return walletJson;
                        }));

                JsonObject walletsData = new JsonObject();
                walletJsonMap.forEach((key, val) -> {
                    String safeKey = firstNonNull(key, "_");
                    walletsData.add(safeKey, val);
                });

                response.add("result", walletsData);
            }
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

    private void initExchangeIfNecessary() {
        try {
            boolean needsInit = exchange.getExchangeMetaData() == null || exchange.getExchangeMetaData().getInstruments() == null;
            if (needsInit) {
                exchange.remoteInit();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize exchange", e);
        }
    }

    private BigDecimal nullToZero(BigDecimal valOrNull) {
        return firstNonNull(valOrNull, BigDecimal.ZERO);
    }
}
