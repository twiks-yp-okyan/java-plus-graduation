package ru.yandex.practicum.ewm.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "similarities",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "event1Id", "event2Id" }) }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Similarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event1_id")
    private Long event1Id;

    @Column(name = "event2_id")
    private Long event2Id;

    @Column(name = "similarity")
    private Double similarity;

    @Column(name = "last_updated_at")
    @Builder.Default
    private LocalDateTime lastUpdatedAt = LocalDateTime.now();
}
