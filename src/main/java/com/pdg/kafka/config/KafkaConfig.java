package com.pdg.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    /**
     * 결제 완료 및 예약 확정 알림용 토픽
     * - 파티션 3개: 컨슈머를 최대 3개까지 늘려 병렬 처리 가능
     * - 복제본 1개: 로컬 환경이므로 단일 브로커 사용
     */
    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name("payment-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * 리뷰 등록/삭제에 따른 평점 재계산용 토픽
     * - 리뷰 데이터는 순서가 중요할 수 있으므로, 필요 시 키(Key) 파티셔닝 전략 고려
     */
    @Bean
    public NewTopic reviewTopic() {
        return TopicBuilder.name("review-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
