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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReservationFacade {

    private final ReservationService reservationService;
    private final ReservationDomainService reservationDomainService;
    private final RoomService roomService;
    private final RoomPriceService roomPriceService;
    private final PaymentService paymentService;
    private final TransactionTemplate writeTx;
    private final TransactionTemplate readOnlyTx;

    public ReservationFacade(ReservationService reservationService,
                             ReservationDomainService reservationDomainService,
                             RoomService roomService,
                             RoomPriceService roomPriceService,
                             PaymentService paymentService,
                             TransactionTemplate writeTransactionTemplate,
                             @Qualifier("readOnlyTransactionTemplate") TransactionTemplate readOnlyTransactionTemplate) {
        this.reservationService = reservationService;
        this.reservationDomainService = reservationDomainService;
        this.roomService = roomService;
        this.roomPriceService = roomPriceService;
        this.paymentService = paymentService;
        this.writeTx = writeTransactionTemplate;
        this.readOnlyTx = readOnlyTransactionTemplate;
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
        // 1. 읽기 검증: 취소할 수 없는 예약에는 PG 취소를 보내지 않는다
        Payment payment = readOnlyTx.execute(status ->
                reservationService.getCancelable(reservationId, memberId).getPayment());

        // 2. PG 취소(트랜잭션 밖)
        paymentService.requestCancel(payment);

        // 3. DB 변경은 한 트랜잭션: 결제 취소, 취소 이력, 예약 취소, 점유 해제
        return writeTx.execute(status -> {
            Reservation reservation = reservationService.getByPublicIdAndMemberId(reservationId, memberId);
            Payment canceledPayment = reservation.getPayment();
            paymentService.markCanceled(canceledPayment, request.getCancelReason());
            reservationService.cancel(reservation);
            return ReservationCancelInfo.from(canceledPayment);
        });
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
