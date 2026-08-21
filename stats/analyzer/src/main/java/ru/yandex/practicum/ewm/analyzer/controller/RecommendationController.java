package ru.yandex.practicum.ewm.analyzer.controller;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.ewm.analyzer.dto.EventScore;
import ru.yandex.practicum.ewm.analyzer.dto.SimilarityDto;
import ru.yandex.practicum.ewm.analyzer.model.projection.EventMaxRating;
import ru.yandex.practicum.ewm.analyzer.service.InteractionService;
import ru.yandex.practicum.ewm.analyzer.service.RecommendationService;
import ru.yandex.practicum.ewm.analyzer.service.SimilarityService;
import ru.yandex.practicum.grpc.controller.RecommendationsControllerGrpc;
import ru.yandex.practicum.grpc.message.InteractionsCountRequestProto;
import ru.yandex.practicum.grpc.message.RecommendedEventProto;
import ru.yandex.practicum.grpc.message.SimilarEventsRequestProto;
import ru.yandex.practicum.grpc.message.UserPredictionsRequestProto;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class RecommendationController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final InteractionService interactionService;
    private final SimilarityService similarityService;
    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto request,
            StreamObserver<RecommendedEventProto> observer
    ) {
        try {
            List<EventScore> recommendedEvents = recommendationService.getRecommendedEventsForUser(
                    request.getUserId(),
                    request.getMaxResults()
            );
            if (recommendedEvents.isEmpty()) {
                observer.onCompleted();
            }
            for (EventScore event : recommendedEvents) {
                RecommendedEventProto response = responseBuilder(event.eventId(), event.score());
                observer.onNext(response);
            }
            observer.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса рекомендаций для пользователя - {}\n{}", request.getUserId(), e.getMessage());
            observer.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto> observer
    ) {
        try {
            List<SimilarityDto> newSimilarEventsForUser = similarityService.getSimilarEventsForUser(
                    request.getUserId(),
                    request.getEventId(),
                    request.getMaxResults()
            );
            for (SimilarityDto similarEventPair : newSimilarEventsForUser) {
                Long newEventId = similarEventPair.event1Id().equals(request.getEventId()) ? similarEventPair.event2Id() : similarEventPair.event1Id();
                RecommendedEventProto response = responseBuilder(newEventId, similarEventPair.similarity());
                observer.onNext(response);
            }
            observer.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса на похожие события от пользователя - {} для события - {}\n{}",
                    request.getUserId(), request.getEventId(), e.getMessage());
            observer.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto request,
            StreamObserver<RecommendedEventProto> observer
    ) {
        try {
            log.debug("Получен запрос на рейтинг для событий {}", request.getEventIdList());
            List<EventMaxRating> eventsMaxRating = interactionService.getEventsMaxRating(request.getEventIdList());
            for (EventMaxRating eventRating : eventsMaxRating) {
                log.debug("Объект рейтинга с айди события - {} и рейтингом - {}", eventRating.getEventId(), eventRating.getRating());
                RecommendedEventProto response = responseBuilder(eventRating.getEventId(), eventRating.getRating());
                observer.onNext(response);
            }
            observer.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при обработке запроса на подсчет рейтинга списка мероприятий\n{}", e.getMessage());
            observer.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    private RecommendedEventProto responseBuilder(Long eventId, Double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }
}
