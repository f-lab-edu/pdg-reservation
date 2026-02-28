package com.pdg.reservation.payment.repository;

import com.pdg.reservation.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface PaymentRepository extends JpaRepository<Payment, Long>, PaymentRepositoryCustom{
}
