package com.pdg.reservation.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate // 생성될 때 자동으로 시간 저장
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // 수정될 때 자동으로 시간 업데이트
    private LocalDateTime updatedAt;
}
