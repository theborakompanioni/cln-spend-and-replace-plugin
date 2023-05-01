package org.tbk.cln.snr.rpc.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.marketdata.params.CurrencyPairsParam;

import java.io.IOException;
import java.util.List;

import static java.util.Optional.ofNullable;

@RequiredArgsConstructor
public class TickerCommand implements RpcCommand {
    @NonNull
    private final Exchange exchange;

    @NonNull
    private final Currency fiatCurrency;

    @Override
    public void execute(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) throws IOException {
        Currency bitcoinCurrency = Currency.BTC;
        Currency fiatCurrency = ofNullable(request.getAsJsonArray("params"))
                .filter(JsonElement::isJsonArray)
                .filter(it -> !it.isEmpty())
                .map(it -> it.get(0))
                .map(it -> it.getAsJsonPrimitive().getAsString())
                .map(Currency::getInstance)
                .orElse(this.fiatCurrency);

        CurrencyPair currencyPair = new CurrencyPair(bitcoinCurrency, fiatCurrency);

        boolean supportedCurrencyPair = exchange.getExchangeInstruments().contains(currencyPair);
        if (!supportedCurrencyPair) {
            throw new IllegalStateException("Currency pair is not supported: " + currencyPair);
        }

        CurrencyPairsParam currencyPairsParam = () -> List.of(currencyPair);
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
    }
}
