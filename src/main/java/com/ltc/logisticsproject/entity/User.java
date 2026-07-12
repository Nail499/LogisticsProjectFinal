package com.ltc.logisticsproject.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;

    @Column(nullable = false, unique = true)
     String username;

    @Column(nullable = false)
     String password; // BCrypt ilə hash olunmuş

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
     Role role;



     Long driverId;
    Long customerId;


    @Column(nullable = false)
     Boolean enabled = true;

     LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.enabled == null) this.enabled = true;
    }
}