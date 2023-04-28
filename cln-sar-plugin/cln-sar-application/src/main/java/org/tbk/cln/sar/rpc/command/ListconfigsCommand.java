package org.tbk.cln.sar.rpc.command;

import com.google.gson.JsonObject;
import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListconfigsCommand implements RpcCommand {

    @NonNull
    private final JsonObject config;

    @Override
    public void execute(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) {
        response.add("result", config);
    }
}
