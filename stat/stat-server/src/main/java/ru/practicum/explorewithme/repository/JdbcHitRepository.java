package ru.practicum.explorewithme.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.practicum.explorewithme.dto.ViewStats;
import ru.practicum.explorewithme.model.HitEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Repository
public class JdbcHitRepository implements HitRepository {
    private static final String INSERT_HIT_SQL = "INSERT INTO stat(app, uri, ip, created) VALUES (?, ?, ?, ?)";

    private static final String SELECT_STATS_SQL = "SELECT app, uri, COUNT(*) AS hits FROM stat " +
            "WHERE created BETWEEN ? AND ?";

    private static final String SELECT_UNIQUE_STATS_SQL =
            "SELECT app, uri, COUNT(DISTINCT ip) AS hits FROM stat " +
                    "WHERE created BETWEEN ? AND ?";

    private final JdbcTemplate jdbcTemplate;

    public JdbcHitRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public HitEntity save(HitEntity hit) {
        jdbcTemplate.update(INSERT_HIT_SQL, hit.app(), hit.uri(), hit.ip(), hit.created());
        return hit;
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        StringBuilder sql = new StringBuilder(unique ? SELECT_UNIQUE_STATS_SQL : SELECT_STATS_SQL);

        List<Object> params = new ArrayList<>();
        params.add(start);
        params.add(end);

        if (uris != null && !uris.isEmpty()) {
            sql.append(" AND uri IN (");
            StringJoiner placeholders = new StringJoiner(", ");
            for (String uri : uris) {
                placeholders.add("?");
                params.add(uri);
            }
            sql.append(placeholders).append(")");
        }

        sql.append(" GROUP BY app, uri ORDER BY hits DESC, app ASC, uri ASC");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new ViewStats(
                rs.getString("app"),
                rs.getString("uri"),
                rs.getLong("hits")
        ), params.toArray());
    }
}
