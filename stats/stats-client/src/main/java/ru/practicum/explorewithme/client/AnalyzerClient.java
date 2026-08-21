package ru.practicum.explorewithme.client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.controller.RecommendationsControllerGrpc;
import ru.yandex.practicum.grpc.message.InteractionsCountRequestProto;
import ru.yandex.practicum.grpc.message.RecommendedEventProto;
import ru.yandex.practicum.grpc.message.SimilarEventsRequestProto;
import ru.yandex.practicum.grpc.message.UserPredictionsRequestProto;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class AnalyzerClient {
    @GrpcClient("analyzer-client")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();
        log.debug("Выполняем запрос рейтинга для событий {}", eventIds);
        Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(request);
        return asStream(iterator);
    }

    public Stream<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, Integer maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        log.debug("Запрос схожих событию - {} незнакомых событий для пользователя - {}", eventId, userId);
        Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);
        return asStream(iterator);
    }

    public Stream<RecommendedEventProto> getRecommendationsForUser(Long userId, Integer maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();
        log.debug("Запрос рекомендаций для пользователя - {}", userId);
        Iterator<RecommendedEventProto> iterator = client.getRecommendationsForUser(request);
        return asStream(iterator);
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
