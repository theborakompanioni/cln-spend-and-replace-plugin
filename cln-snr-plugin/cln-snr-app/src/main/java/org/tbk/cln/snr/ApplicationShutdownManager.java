package org.tbk.cln.snr;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

@RequiredArgsConstructor
class ApplicationShutdownManager {

    @NonNull
    private final ApplicationContext context;

    /**
     * Invoke with `0` to indicate no error or different code to indicate abnormal exit.
     */
    public int initiateShutdown(int returnCode) {
        return SpringApplication.exit(context, () -> returnCode);
    }
}