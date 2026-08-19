package ru.yandex.practicum.ewm.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "interactions",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "userId", "eventId" }) }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Interaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "last_updated_at")
    @Builder.Default
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();
}
