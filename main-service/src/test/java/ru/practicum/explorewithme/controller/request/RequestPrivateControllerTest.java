package ru.practicum.explorewithme.controller.request;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.practicum.explorewithme.dto.request.ParticipationRequestDto;
import ru.practicum.explorewithme.model.request.Status;
import ru.practicum.explorewithme.service.request.RequestService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequestPrivateController.class)
class RequestPrivateControllerTest {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RequestService requestService;

    @Test
    void getUserRequestsShouldReturnRequestsCorrectly() throws Exception {
        Long userId = 10L;
        String created = LocalDateTime.now().format(formatter);

        ParticipationRequestDto result = new ParticipationRequestDto(
                20L,
                created,
                30L,
                userId,
                Status.PENDING.name());

        List<ParticipationRequestDto> expectedResult = List.of(result);

        Mockito.when(requestService.getUserRequests(userId)).thenReturn(expectedResult);

        mockMvc.perform(MockMvcRequestBuilders.get("/users/10/requests"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[*]").exists())
                .andExpect(jsonPath("$[1]").doesNotExist())
                .andExpect(jsonPath("$.[0].id").value(20))
                .andExpect(jsonPath("$.[0].created").value(created))
                .andExpect(jsonPath("$.[0].event").value(30))
                .andExpect(jsonPath("$.[0].requester").value(userId))
                .andExpect(jsonPath("$.[0].status").value(Status.PENDING.name()));

        Mockito.verify(requestService, Mockito.times(1)).getUserRequests(Mockito.eq(userId));
    }

    @Test
    void addUserRequestShouldCreateRequestsCorrectly() throws Exception {
        Long userId = 20L;
        Long eventId = 30L;
        String created = LocalDateTime.now().format(formatter);

        ParticipationRequestDto result = new ParticipationRequestDto(
                10L,
                created,
                30L,
                userId,
                Status.PENDING.name());

        Mockito.when(requestService.addUserRequest(userId, eventId)).thenReturn(result);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/20/requests")
                        .param("eventId", "30"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.created").value(created))
                .andExpect(jsonPath("$.event").value(30))
                .andExpect(jsonPath("$.requester").value(userId))
                .andExpect(jsonPath("$.status").value(Status.PENDING.name()));

        Mockito.verify(requestService, Mockito.times(1))
                .addUserRequest(Mockito.eq(userId), Mockito.eq(eventId));
    }

    @Test
    void addUserRequestShouldThrowBadRequestWhenUserIdNotLong() throws Exception {
        Long eventId = 30L;

        mockMvc.perform(MockMvcRequestBuilders.post("/users/trtr/requests")
                        .param("eventId", "30"))
                .andExpect(status().isBadRequest());

        Mockito.verify(requestService, Mockito.times(0))
                .addUserRequest(Mockito.any(), Mockito.eq(eventId));
    }

    @Test
    void addUserRequestShouldThrowBadRequestWhenEventIdNotLong() throws Exception {
        Long userId = 20L;

        mockMvc.perform(MockMvcRequestBuilders.post("/users/trtr/requests")
                        .param("eventId", "test"))
                .andExpect(status().isBadRequest());

        Mockito.verify(requestService, Mockito.times(0))
                .addUserRequest(Mockito.eq(userId), Mockito.any());
    }

    @Test
    void updateUserRequestShouldUpdateRequestsCorrectly() throws Exception {
        Long userId = 20L;
        Long eventId = 30L;
        Long requestId = 10L;
        String created = LocalDateTime.now().format(formatter);

        ParticipationRequestDto result = new ParticipationRequestDto(
                requestId,
                created,
                eventId,
                userId,
                Status.CANCELED.name());

        Mockito.when(requestService.rejectUserRequest(userId, requestId)).thenReturn(result);

        mockMvc.perform(MockMvcRequestBuilders.patch("/users/20/requests/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.created").value(created))
                .andExpect(jsonPath("$.event").value(30))
                .andExpect(jsonPath("$.requester").value(userId))
                .andExpect(jsonPath("$.status").value(Status.CANCELED.name()));

        Mockito.verify(requestService, Mockito.times(1))
                .rejectUserRequest(Mockito.eq(userId), Mockito.eq(requestId));
    }

    @Test
    void updateUserRequestShouldThrowBadRequestWhenUserIdNotLong() throws Exception {
        Long requestId = 10L;

        mockMvc.perform(MockMvcRequestBuilders.patch("/users/dsads/requests/10/cancel"))
                .andExpect(status().isBadRequest());

        Mockito.verify(requestService, Mockito.times(0))
                .rejectUserRequest(Mockito.any(), Mockito.eq(requestId));
    }

    @Test
    void updateUserRequestShouldThrowBadRequestWhenRequestIdNotLong() throws Exception {
        Long userId = 20L;

        mockMvc.perform(MockMvcRequestBuilders.patch("/users/20/requests/trtr/cancel"))
                .andExpect(status().isBadRequest());

        Mockito.verify(requestService, Mockito.times(0))
                .addUserRequest(Mockito.eq(userId), Mockito.any());
    }
}
