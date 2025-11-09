package com.hungerexpress.admin;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "admin_audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actorEmail; // admin who performed the action

    @Column(nullable = false)
    private String action; // e.g. USER_ROLE_CHANGE, USER_ENABLE, RESTAURANT_APPROVE, ORDER_STATUS_OVERRIDE

    @Column(length = 200)
    private String target; // e.g. user:123, restaurant:45, order:789

    @Column(length = 1000)
    private String details; // additional context

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
