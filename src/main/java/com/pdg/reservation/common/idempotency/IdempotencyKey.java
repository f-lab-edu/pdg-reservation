package com.pdg.reservation.common.idempotency;

import com.pdg.reservation.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 클라이언트가 보낸 멱등성 키를 "이미 썼다"고 기록하는 행. 상태도 응답도 담지 않는다.
// 같은 키가 다시 오면 중복 요청으로 거절하고, 실패한 뒤에도 키를 지우지 않으므로 클라이언트는 새 키로 다시 시도한다.
@Entity
@Table(name = "idempotency_key",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_idempotency_key_key_value", columnNames = "key_value")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_value", nullable = false, updatable = false, length = 64)
    private String keyValue;
}
