package com.pdg.reservation.member.entity;

import com.pdg.reservation.common.entity.BaseEntity;
import com.pdg.reservation.member.enums.Grade;
import com.pdg.reservation.member.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Builder
@DynamicInsert // null인 필드는 insert 쿼리에서 제외시킴 -> DB Default값 발동
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, length = 13)
    private String phoneNumber;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'BRONZE'")
    @Builder.Default
    private Grade grade = Grade.BRONZE;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'NORMAL'")
    @Builder.Default
    private Role role = Role.NORMAL;

    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private boolean isActive = false;

    private LocalDateTime deletedAt;

}
