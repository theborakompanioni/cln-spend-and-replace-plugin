package org.tbk.cln.snr.rpc.command;

import com.google.gson.JsonObject;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class VersionCommand implements RpcCommand {

    @Override
    public void execute(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        JsonObject result = new JsonObject();
        result.addProperty("version", Optional.of(this.getClass().getPackage())
                .map(Package::getImplementationVersion)
                .orElse("local"));

        response.add("result", result);
    }
}
