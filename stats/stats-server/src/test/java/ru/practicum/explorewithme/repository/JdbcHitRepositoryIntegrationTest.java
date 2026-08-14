package ru.practicum.explorewithme.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.explorewithme.dto.ViewStats;
import ru.practicum.explorewithme.model.HitEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(JdbcHitRepository.class)
@ActiveProfiles("test")
class JdbcHitRepositoryIntegrationTest {

    @Autowired
    private JdbcHitRepository repository;

    @Test
    void saveShouldPersistRecord() {
        LocalDateTime now = LocalDateTime.now();
        HitEntity hit = new HitEntity(null, "ewm-main-service", "/events/1", "192.168.0.1", now);

        repository.save(hit);

        List<ViewStats> stats = repository.getStats(now.minusMinutes(1), now.plusMinutes(1), List.of("/events/1"), false);
        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst().hits()).isEqualTo(1L);
    }

    @Test
    void getStatsShouldReturnNonUniqueCount() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(new HitEntity(null, "ewm-main-service", "/events/1", "192.168.0.1", now));
        repository.save(new HitEntity(null, "ewm-main-service", "/events/1", "192.168.0.1", now.plusSeconds(1)));

        List<ViewStats> stats = repository.getStats(now.minusMinutes(1), now.plusMinutes(1), List.of("/events/1"), false);

        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst().app()).isEqualTo("ewm-main-service");
        assertThat(stats.getFirst().uri()).isEqualTo("/events/1");
        assertThat(stats.getFirst().hits()).isEqualTo(2L);
    }

    @Test
    void getStatsShouldReturnUniqueCount() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(new HitEntity(null, "ewm-main-service", "/events/1", "192.168.0.1", now));
        repository.save(new HitEntity(null, "ewm-main-service", "/events/1", "192.168.0.1", now.plusSeconds(1)));
        repository.save(new HitEntity(null, "ewm-main-service", "/events/1", "192.168.0.2", now.plusSeconds(2)));

        List<ViewStats> stats = repository.getStats(now.minusMinutes(1), now.plusMinutes(1), List.of("/events/1"), true);

        assertThat(stats).hasSize(1);
        assertThat(stats.getFirst().hits()).isEqualTo(2L);
    }

    @Test
    void getStatsShouldReturnOnlyRequestedUrisWithinRange() {
        LocalDateTime now = LocalDateTime.now();
        repository.save(new HitEntity(null, "ewm-main-service", "/events/1", "192.168.0.1", now));
        repository.save(new HitEntity(null, "ewm-main-service", "/events/2", "192.168.0.2", now));
        repository.save(new HitEntity(null, "ewm-main-service", "/events/3", "192.168.0.3", now.minusHours(5)));

        List<ViewStats> stats = repository.getStats(now.minusMinutes(1), now.plusMinutes(1), List.of("/events/1", "/events/2"), false);

        assertThat(stats).hasSize(2);
        assertThat(stats).extracting(ViewStats::uri).containsExactlyInAnyOrder("/events/1", "/events/2");
    }
}
