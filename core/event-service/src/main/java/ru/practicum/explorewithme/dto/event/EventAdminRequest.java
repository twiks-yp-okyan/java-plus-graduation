package ru.practicum.explorewithme.dto.event;

import java.util.List;

public record EventAdminRequest(List<Long> users,
                                List<String> states,
                                List<Long> categories,
                                String rangeStart,
                                String rangeEnd) {
}