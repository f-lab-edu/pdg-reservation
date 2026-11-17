package com.pdg.reservation.reservation.facade;

import com.pdg.reservation.common.annotation.DistributedLock;
import com.pdg.reservation.member.entity.Member;
import com.pdg.reservation.payment.entity.Payment;
import com.pdg.reservation.payment.service.PaymentService;
import com.pdg.reservation.reservation.dto.ReservationCancelInfo;
import com.pdg.reservation.reservation.dto.ReservationCancelRequest;
import com.pdg.reservation.reservation.dto.ReservationInfo;
import com.pdg.reservation.reservation.dto.ReservationRequest;
import com.pdg.reservation.reservation.dto.ReservationSearchCondition;
import com.pdg.reservation.common.dto.PageInfo;
import com.pdg.reservation.reservation.dto.ReservationSearchInfo;
import com.pdg.reservation.reservation.entity.Reservation;
import com.pdg.reservation.reservation.service.ReservationDomainService;
import com.pdg.reservation.reservation.service.ReservationService;
import com.pdg.reservation.room.entity.Room;
import com.pdg.reservation.room.price.StayPrice;
import com.pdg.reservation.room.service.RoomPriceService;
import com.pdg.reservation.room.service.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ReservationFacade {

    private final ReservationService reservationService;
    private final ReservationDomainService reservationDomainService;
    private final RoomService roomService;
    private final RoomPriceService roomPriceService;
    private final PaymentService paymentService;
    private final TransactionTemplate writeTx;

    public ReservationFacade(ReservationService reservationService,
                             ReservationDomainService reservationDomainService,
                             RoomService roomService,
                             RoomPriceService roomPriceService,
                             PaymentService paymentService,
                             TransactionTemplate writeTransactionTemplate) {
        this.reservationService = reservationService;
        this.reservationDomainService = reservationDomainService;
        this.roomService = roomService;
        this.roomPriceService = roomPriceService;
        this.paymentService = paymentService;
        this.writeTx = writeTransactionTemplate;
    }

    @Transactional
    public ReservationInfo reserve(Member member, ReservationRequest request) {
        Long roomId = request.getRoomId();
        LocalDate checkIn = request.getCheckInDate();
        LocalDate checkOut = request.getCheckOutDate();

        Room room = roomService.getRoom(roomId);
        boolean blocked = roomService.isBlocked(roomId, checkIn, checkOut);
        boolean occupied = reservationService.isOccupied(roomId, checkIn, checkOut);
        StayPrice stayPrice = roomPriceService.calculateStayPrice(room, checkIn, checkOut);
        reservationDomainService.validateReserve(member, request, room, blocked, occupied, stayPrice);

        Reservation reservation = reservationService.reserve(member, room, request, stayPrice);
        return ReservationInfo.from(reservation);
    }

    // 외부 호출(PG 취소)이 있어서 메서드에는 트랜잭션을 두지 않고, DB 구간만 TransactionTemplate으로 묶는다
    @DistributedLock(key = "'reserveCancel:reservationId:' + #reservationId")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReservationCancelInfo cancel(Long memberId, UUID reservationId, ReservationCancelRequest request) {
        // 1. 취소할 수 있는 예약인지 확인하고, PG를 부르기 전에 예약과 결제를 "취소 진행 중"으로 바꿔 커밋한다
        //    이 쓰기가 실패하면 PG를 아직 부르지 않았으므로 아무 일도 일어나지 않는다
        Payment payment = writeTx.execute(status -> {
            Reservation reservation = reservationService.getCancelable(reservationId, memberId);
            Payment target = reservation.getPayment();
            reservationService.startCancel(reservation);
            paymentService.startCancel(target);
            return target;
        });

        // 2. PG 취소(트랜잭션 밖). PG가 거절하면 원래 상태로 되돌리고 오류를 그대로 올린다
        try {
            paymentService.requestCancel(payment);
        } catch (Exception e) {
            revertCancel(memberId, reservationId);
            throw e;
        }

        // 3. DB 확정은 한 트랜잭션: 결제 취소, 취소 이력, 예약 취소, 점유 해제
        //    여기서 실패하면 "취소 진행 중"으로 남는다. PG 조회가 없어 자동 복구는 하지 않고 수동으로 확인한다
        try {
            return writeTx.execute(status -> {
                Reservation reservation = reservationService.getByPublicIdAndMemberId(reservationId, memberId);
                Payment canceledPayment = reservation.getPayment();
                paymentService.markCanceled(canceledPayment, request.getCancelReason());
                reservationService.cancel(reservation);
                return ReservationCancelInfo.from(canceledPayment);
            });
        } catch (Exception e) {
            log.error("PG 취소는 끝났지만 DB 확정에 실패해 취소 진행 중으로 남음. 수동 확인 필요. 예약ID: {}", reservationId, e);
            throw e;
        }
    }

    // PG가 취소를 거절했을 때 예약과 결제를 원래 상태로 되돌린다
    private void revertCancel(Long memberId, UUID reservationId) {
        try {
            writeTx.executeWithoutResult(status -> {
                Reservation reservation = reservationService.getByPublicIdAndMemberId(reservationId, memberId);
                reservationService.revertCancel(reservation);
                paymentService.revertCancel(reservation.getPayment());
            });
        } catch (Exception e) {
            log.error("PG 취소 거절 뒤 원복에 실패해 취소 진행 중으로 남음. 수동 확인 필요. 예약ID: {}", reservationId, e);
        }
    }

    // 결제 제한시간이 지난 예약을 만료 처리한다
    @Transactional
    public void expire(Long reservationId) {
        reservationService.rollbackReservation(reservationId);
    }

    public PageInfo<ReservationSearchInfo> search(Long memberId, ReservationSearchCondition condition, Pageable pageable) {
        return PageInfo.from(reservationService.search(memberId, condition, pageable));
    }
}
