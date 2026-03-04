package com.pdg.reservation.payment.repository;

import com.pdg.reservation.payment.entity.PaymentCancelHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCancelHistoryRepository extends JpaRepository<PaymentCancelHistory, Long> {
}
