package com.pdg.reservation.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

// 외부 호출(PG 등)이 있는 Facade 메서드가 DB 구간만 직접 트랜잭션으로 묶을 때 쓴다(architecture-guide 5절)
@Configuration
public class TransactionConfig {

    @Bean
    @Primary
    public TransactionTemplate writeTransactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    @Bean
    public TransactionTemplate readOnlyTransactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(true);
        return template;
    }
}
