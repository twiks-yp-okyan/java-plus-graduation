package ru.practicum.explorewithme.client;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.controller.UserActionControllerGrpc;
import ru.yandex.practicum.grpc.message.ActionTypeProto;
import ru.yandex.practicum.grpc.message.UserActionProto;

import java.time.Instant;

@Service
@Slf4j
public class CollectorClient {
    @GrpcClient("collector-client")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void sendView(Long eventId, Long userId) {
        sendUserAction(eventId, userId, ActionTypeProto.ACTION_VIEW);
    }

    public void sendRegister(Long eventId, Long userId) {
        sendUserAction(eventId, userId, ActionTypeProto.ACTION_REGISTER);
    }

    public void sendLike(Long eventId, Long userId) {
        sendUserAction(eventId, userId, ActionTypeProto.ACTION_LIKE);
    }

    private void sendUserAction(Long eventId, Long userId, ActionTypeProto actionType) {
        Instant now = Instant.now();
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build();

        client.collectUserAction(request);
        log.debug("Действие пользователя для события - {} отправлено в Collector через клиент: {}", eventId, actionType);
    }
}
