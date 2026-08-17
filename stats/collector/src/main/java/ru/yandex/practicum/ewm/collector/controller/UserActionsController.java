package ru.yandex.practicum.ewm.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.ewm.collector.service.UserActionHandler;
import ru.yandex.practicum.grpc.recommendation.controller.user.action.UserActionControllerGrpc;
import ru.yandex.practicum.grpc.recommendation.message.user.action.UserActionProto;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class UserActionsController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionHandler handler;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.debug("Получено событие после действия пользователя - {}", request);
            handler.handle(request);
            // после обработки события возвращаем ответ клиенту
            responseObserver.onNext(Empty.getDefaultInstance());
            // и завершаем обработку запроса
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при обработке события после действия пользователя - {}", request);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
