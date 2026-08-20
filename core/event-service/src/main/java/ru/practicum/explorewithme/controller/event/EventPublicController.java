package ru.practicum.explorewithme.controller.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.dto.event.EventFullDto;
import ru.practicum.explorewithme.dto.event.EventShortDto;
import ru.practicum.explorewithme.service.event.EventService;
import ru.practicum.explorewithme.utils.HttpHeadersConstants;

import java.util.List;

@RestController
@Validated
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventPublicController {
    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(@RequestParam(required = false) @Size(min = 1, max = 7000) String text,
                                         @RequestParam(required = false) List<Long> categories,
                                         @RequestParam(required = false) Boolean paid,
                                         @RequestParam(required = false) String rangeStart,
                                         @RequestParam(required = false) String rangeEnd,
                                         @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                         @RequestParam(required = false) String sort,
                                         @RequestParam(defaultValue = "0") @Min(0) int from,
                                         @RequestParam(defaultValue = "10") @Min(1) int size,
                                         HttpServletRequest request) {
        return eventService.searchPublicEvents(
                text,
                categories,
                paid,
                rangeStart,
                rangeEnd,
                onlyAvailable,
                sort,
                from,
                size,
                request.getRequestURI(),
                request.getRemoteAddr()
        );
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(
            @RequestHeader(value = HttpHeadersConstants.X_EWM_USER_ID, required = false) Long userId,
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        return eventService.getPublishedEventById(userId, id, request.getRequestURI(), request.getRemoteAddr());
    }

    @GetMapping("/recommendations")
    public List<EventFullDto> getUserRecommendations(
            @RequestHeader(value = HttpHeadersConstants.X_EWM_USER_ID, required = false) Long userId
    ) {
        return eventService.getUserRecommendations(userId);
    }

    @PutMapping("/{eventId}/like")
    public void setUserEventLike(
            @PathVariable("eventId") Long eventId,
            @RequestHeader(value = HttpHeadersConstants.X_EWM_USER_ID, required = false) Long userId
    ) {
        eventService.sendUserEventLike(userId, eventId);
    }
}
