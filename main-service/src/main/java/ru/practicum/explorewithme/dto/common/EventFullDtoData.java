package ru.practicum.explorewithme.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.practicum.explorewithme.model.category.Category;
import ru.practicum.explorewithme.model.event.Event;
import ru.practicum.explorewithme.model.location.Location;
import ru.practicum.explorewithme.model.user.User;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventFullDtoData {
    private Event event;
    private Category category;
    private User initiator;
    private Location location;
}
