package ru.yandex.practicum.request.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.request.dto.event.EventFullDto;

@FeignClient(name = "main-service")
public interface EventClient {
    @GetMapping("/api/events/{id}")
    EventFullDto getById(@PathVariable("id") Long id);
}
