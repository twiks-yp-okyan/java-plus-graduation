package ru.yandex.practicum.comment.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "request-service")
public interface RequestClient {
    @GetMapping("/api/requests/confirmed/{eventId}/{userId}")
    boolean checkUserRequestConfirmation(
            @PathVariable("eventId") Long eventId,
            @PathVariable("userId") Long userId
    );
}