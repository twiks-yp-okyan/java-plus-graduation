package ru.yandex.practicum.comment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.yandex.practicum.comment.dto.event.EventShortDto;

@FeignClient(name = "main-service")
public interface EventClient {
    @GetMapping("/api/events/{id}")
    EventShortDto getById(@PathVariable("id") Long id);
}
