package ru.yandex.practicum.ewm.analyzer.service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.ewm.analyzer.model.Interaction;
import ru.yandex.practicum.ewm.analyzer.model.UserActionType;
import ru.yandex.practicum.ewm.analyzer.repository.InteractionRepository;
import ru.yandex.practicum.ewm.analyzer.utils.EnumMapper;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActionHandlerImpl implements UserActionHandler {
    private final InteractionRepository repository;
    @Value("${user.action.weight.like}")
    private final Double LIKE_WEIGHT;
    @Value("${user.action.weight.register}")
    private final Double REGISTER_WEIGHT;
    @Value("${user.action.weight.view}")
    private final Double VIEW_WEIGHT;

    @Override
    public void handle(UserActionAvro event) {
        Interaction interaction;
        Optional<Interaction> currentInteraction = repository.findByUserIdAndEventId(event.getUserId(), event.getEventId());
        if (currentInteraction.isPresent()) {
            interaction = currentInteraction.get();
            log.debug("Потенциальное обновление Interaction для userId - {} & eventId - {}",
                    interaction.getUserId(), interaction.getEventId());
            Double newRating = mapRating(EnumMapper.map(event.getActionType(), UserActionType.class));
            if (Double.compare(newRating, interaction.getRating()) > 0) {
                interaction.setRating(newRating);
            }
        } else {
            log.debug("Новый Interaction для userId - {} & eventId - {}",
                    event.getUserId(), event.getEventId());
            interaction = Interaction.builder()
                    .userId(event.getUserId())
                    .eventId(event.getEventId())
                    .rating(mapRating(EnumMapper.map(event.getActionType(), UserActionType.class)))
                    .build();
        }
        repository.save(interaction);
    }

    private Double mapRating(UserActionType actionType) {
        return switch (actionType) {
            case LIKE -> LIKE_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case VIEW -> VIEW_WEIGHT;
        };
    }
}
