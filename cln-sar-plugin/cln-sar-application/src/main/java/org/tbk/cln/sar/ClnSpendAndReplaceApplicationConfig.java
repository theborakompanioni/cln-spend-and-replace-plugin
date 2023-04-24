package org.tbk.cln.sar;

import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.Exchange;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class ClnSpendAndReplaceApplicationConfig {

    @Bean
    public ApplicationShutdownManager applicationShutdownManager(ApplicationContext applicationContext) {
        return new ApplicationShutdownManager(applicationContext);
    }

    @Bean(initMethod = "start")
    public ClnSpendAndReplacePlugin clnSpendAndReplacePlugin(ApplicationShutdownManager applicationShutdownManager,
                                                             Exchange exchange) {
        return new ClnSpendAndReplacePlugin(applicationShutdownManager, exchange);
    }
}
