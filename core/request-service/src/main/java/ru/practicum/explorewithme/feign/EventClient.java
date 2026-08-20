package ru.practicum.explorewithme.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.explorewithme.dto.event.EventFullDto;

@FeignClient(name = "event-service")
public interface EventClient {
    @GetMapping("/api/events/{id}")
    EventFullDto getById(@PathVariable("id") Long id);
}
