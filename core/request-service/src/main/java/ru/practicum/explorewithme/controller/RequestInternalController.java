package ru.practicum.explorewithme.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.service.RequestService;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RequestInternalController {
    private final RequestService requestService;

    @GetMapping("/api/requests/confirmed")
    public Map<Long, Long> getConfirmedRequestsCountByEventIds(@RequestParam(value = "eventIds") List<Long> eventIds) {
        return requestService.countConfirmedRequestsByEventIds(new HashSet<>(eventIds));
    }

    @GetMapping("/api/requests/confirmed/{eventId}")
    public Integer getConfirmedRequestsCountByEventId(@PathVariable("eventId") Long eventId) {
        return requestService.countConfirmedRequestsByEventId(eventId);
    }

    @GetMapping("/api/requests")
    public Map<Long, Long> getRequestsCountByEventIds(@RequestParam(value = "eventIds") List<Long> eventIds) {
        return requestService.countRequestsByEventIds(new HashSet<>(eventIds));
    }

    @GetMapping("/api/requests/{eventId}")
    public Integer getRequestsCountByEventId(@PathVariable("eventId") Long eventId) {
        return requestService.countRequestsByEventId(eventId);
    }

    @GetMapping("/api/requests/confirmed/{eventId}/{userId}")
    public boolean checkUserRequestConfirmation(
            @PathVariable("eventId") Long eventId,
            @PathVariable("userId") Long userId
    ) {
        return requestService.checkUserRequestConfirmation(eventId, userId);
    }
}
