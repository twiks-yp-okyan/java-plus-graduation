package ru.practicum.explorewithme.controller.event;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.explorewithme.dto.event.EventFullDto;
import ru.practicum.explorewithme.service.event.EventService;

@RestController
@RequiredArgsConstructor
public class EventInternalController {
    private final EventService eventService;

    @GetMapping("/api/events/{id}")
    public EventFullDto getById(@PathVariable("id") Long id) {
        return eventService.getById(id);
    }
}
