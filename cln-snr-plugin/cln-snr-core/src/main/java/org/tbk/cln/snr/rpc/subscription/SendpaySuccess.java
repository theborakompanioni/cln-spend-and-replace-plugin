package org.tbk.cln.snr.rpc.subscription;

import fr.acinq.lightning.MilliSatoshi;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.clightning.plugins.log.PluginLog;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Wallet;
import org.tbk.cln.snr.RunOptions;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

@RequiredArgsConstructor
public class SendpaySuccess implements ClnSubscription {

    @NonNull
    private final Exchange exchange;

    @NonNull
    private final Currency fiatCurrency;

    @NonNull
    private final RunOptions runOptions;

    @Override
    public void execute(ICLightningPlugin plugin, CLightningJsonObject data) throws Exception {
        plugin.log(PluginLog.DEBUG, data);

        MilliSatoshi amountSent = Optional.ofNullable(data.getAsJsonObject("params"))
                .map(it -> it.getAsJsonObject("sendpay_success"))
                .map(it -> it.getAsJsonPrimitive("msatoshi_sent"))
                .map(it -> new MilliSatoshi(it.getAsLong()))
                .orElseThrow(() -> new IllegalStateException("Could no"));

        plugin.log(PluginLog.DEBUG, "Spent amount which needs to be replaced: " + amountSent);

        if (runOptions.isDryRun()) {
            plugin.log(PluginLog.INFO, String.format("""
                    === DRY RUN is active ===
                    would have replaced: %s
                    """, amountSent));
        } else {
            CurrencyPair currencyPair = new CurrencyPair(Currency.BTC, fiatCurrency);
            boolean supportedCurrencyPair = exchange.getExchangeInstruments().contains(currencyPair);
            if (!supportedCurrencyPair) {
                throw new IllegalStateException("Currency pair is not supported: " + currencyPair);
            }

            BigDecimal availableFiatBalance = fetchAvailableFiatBalance();
            plugin.log(PluginLog.DEBUG, "Available fiat amount on exchange: " + availableFiatBalance);

            plugin.log(PluginLog.ERROR, "Error while replacing amount: Not implemented yet");
        }
    }

    private BigDecimal fetchAvailableFiatBalance() throws IOException {
        return exchange.getAccountService().getAccountInfo()
                .getWallet(Wallet.WalletFeature.TRADING)
                .getBalance(this.fiatCurrency)
                .getAvailable();
    }
}
