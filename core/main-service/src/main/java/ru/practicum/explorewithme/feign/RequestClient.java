package ru.practicum.explorewithme.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;
import java.util.Set;

@FeignClient(name = "request-service")
public interface RequestClient {
    @GetMapping("/api/requests/confirmed")
    Map<Long, Integer> getConfirmedRequestsCountByEventIds(@RequestParam(value = "eventIds") Set<Long> eventIds);

    @GetMapping("/api/requests/confirmed/{eventId}")
    Integer getConfirmedRequestsCountByEventId(@PathVariable("eventId") Long eventId);

    @GetMapping("/api/requests")
    Map<Long, Integer> getRequestsCountByEventIds(@RequestParam(value = "eventIds") Set<Long> eventIds);

    @GetMapping("/api/requests/{eventId}")
    Integer getRequestsCountByEventId(@PathVariable("eventId") Long eventId);

    @GetMapping("/api/requests/confirmed/{eventId}/{userId}")
    boolean checkUserRequestConfirmation(
            @PathVariable("eventId") Long eventId,
            @PathVariable("userId") Long userId
    );
}
