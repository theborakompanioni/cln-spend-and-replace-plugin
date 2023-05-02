package org.tbk.cln.snr;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RunOptions {
    boolean dryRun;
}