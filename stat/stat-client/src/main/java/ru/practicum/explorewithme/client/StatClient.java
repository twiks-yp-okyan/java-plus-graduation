package ru.practicum.explorewithme.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.explorewithme.dto.EndpointHit;
import ru.practicum.explorewithme.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class StatClient {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;

    public StatClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void saveHit(EndpointHit hitDto) {
        restTemplate.postForObject("/hit", hitDto, Void.class);
        log.info("Сохранено обращение: {}", hitDto);
    }

    public List<ViewStats> getStat(
            LocalDateTime start,
            LocalDateTime end,
            List<String> uris,
            boolean unique
    ) {
        log.info("start={}, end={}, uris={}, unique={}", start, end, uris, unique);
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/stats")
                .queryParam("start", start.format(formatter).replace(" ", "+"))
                .queryParam("end", end.format(formatter).replace(" ", "+"))
                .queryParam("unique", unique);
        log.info("builder={}", builder);

        if (uris != null && !uris.isEmpty()) {
            uris.forEach(uri -> builder.queryParam("uris", uri));
        }

        log.info("builder={}", builder);

        ViewStats[] response = restTemplate
                .getForObject(builder.toUriString(), ViewStats[].class);
        log.info("response", response);
        return response != null ? Arrays.asList(response) : List.of();
    }
}