package org.tbk.cln.snr.rpc.command;

import com.google.gson.JsonObject;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.dto.meta.InstrumentMetaData;
import org.knowm.xchange.instrument.Instrument;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class ExchangeInfoCommand implements RpcCommand {

    private final Exchange exchange;

    private final Set<Instrument> instruments;

    public ExchangeInfoCommand(Exchange exchange, Set<Instrument> instruments) {
        this.exchange = requireNonNull(exchange);
        this.instruments = Collections.unmodifiableSet(instruments);
    }

    @Override
    public void execute(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) throws IOException {
        ExchangeSpecification spec = exchange.getExchangeSpecification();

        JsonObject result = new JsonObject();
        result.addProperty("name", spec.getExchangeName());
        result.addProperty("description", spec.getExchangeDescription());
        result.addProperty("host", spec.getHost());

        ExchangeMetaData metadata = exchange.getExchangeMetaData();
        JsonObject metadataJson = new JsonObject();

        JsonObject metadataInstrumentsJson = new JsonObject();
        metadata.getInstruments().entrySet().stream()
                .filter(entry -> instruments.contains(entry.getKey()))
                .forEach(entry -> {
                    InstrumentMetaData value = entry.getValue();
                    JsonObject data = new JsonObject();
                    data.addProperty("min-amount", value.getMinimumAmount().toPlainString());
                    metadataInstrumentsJson.add(entry.getKey().toString(), data);
                });

        metadataJson.add("instruments", metadataInstrumentsJson);
        result.add("metadata", metadataJson);

        response.add("result", result);
    }
}
