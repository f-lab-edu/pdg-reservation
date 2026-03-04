package com.pdg.reservation.reservation.listener;

import com.pdg.reservation.reservation.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReservationTimeoutListener extends KeyExpirationEventMessageListener {

    private final ReservationService reservationService;

    public ReservationTimeoutListener(
            RedisMessageListenerContainer listenerContainer, ReservationService reservationService
    ) {
        super(listenerContainer);
        this.reservationService = reservationService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();

        try {
            // pending_reservation:123 형식인지 체크
            if (expiredKey.startsWith("pending_reservation:")) {
                Long reservationId = Long.parseLong(expiredKey.split(":")[1]);
                log.info("결제 시간 만료, 롤백 프로세스 시작: ReservationId={}", reservationId);
                reservationService.rollbackReservation(reservationId);
            }
        } catch (Exception e) {
            log.error("결제 만료 롤백 실패. 수동 확인 필요. Key: {}", message, e);
        }

    }
}
