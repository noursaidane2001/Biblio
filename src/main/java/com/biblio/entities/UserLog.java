package com.biblio.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_logs", indexes = {
        @Index(name = "idx_userlog_created_at", columnList = "createdAt"),
        @Index(name = "idx_userlog_action", columnList = "action"),
        @Index(name = "idx_userlog_user", columnList = "utilisateur_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private User utilisateur;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String level = "INFO";

    @Column(length = 45)
    private String ip;

    @Column(length = 255)
    private String userAgent;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

