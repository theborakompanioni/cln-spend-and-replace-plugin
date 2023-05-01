package org.tbk.cln.snr.rpc.command;

import jrpc.clightning.plugins.ICLightningPlugin;
import jrpc.service.converters.jsonwrapper.CLightningJsonObject;

public interface RpcCommand {
    void execute(ICLightningPlugin plugin, CLightningJsonObject request, CLightningJsonObject response) throws Exception;
}
