package org.tbk.cln.sar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class ClnSpendAndReplaceApplicationConfig {

    @Bean(initMethod = "start")
    public ClnSpendAndReplacePlugin plugin(ApplicationContext applicationContext) {
        ApplicationShutdownManager applicationShutdownManager = new ApplicationShutdownManager(applicationContext);
        return new ClnSpendAndReplacePlugin(applicationShutdownManager);
    }
}
