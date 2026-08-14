package ru.practicum.explorewithme.controller.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.practicum.explorewithme.dto.comment.CommentDto;
import ru.practicum.explorewithme.dto.comment.NewComment;
import ru.practicum.explorewithme.service.comment.CommentService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@WebMvcTest(CommentPrivateController.class)
class CommentPrivateControllerTest {
    private final DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private CommentService commentService;

    @Test
    void createShouldCreateCommentCorrectly() throws Exception {
        NewComment newComment = new NewComment("test text".repeat(20));
        String dateTime = LocalDateTime.now().format(customFormatter);
        CommentDto expectedComment = CommentDto.builder()
                .id(1L)
                .text("test text".repeat(20))
                .user(10L)
                .event(20L)
                .createdOn(dateTime)
                .build();

        Mockito.when(commentService.create(10L, 20L, newComment)).thenReturn(expectedComment);

        String requestContent = mapper.writeValueAsString(newComment);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/10/comments/events/20")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.text").value("test text".repeat(20)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.user").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.event").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.createdOn").value(dateTime));

        Mockito.verify(commentService, Mockito.times(1)).create(
                Mockito.eq(10L), Mockito.eq(20L), Mockito.eq(newComment));
    }

    @Test
    void createShouldThrowBadRequestWhenTextLessThan20() throws Exception {
        NewComment newComment = new NewComment("first_".repeat(3));

        String requestContent = mapper.writeValueAsString(newComment);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/10/comments/events/20")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        Mockito.verify(commentService, Mockito.never()).create(
                Mockito.eq(10L), Mockito.eq(20L), Mockito.eq(newComment));
    }

    @Test
    void createShouldThrowBadRequestWhenTextMoreThan2000() throws Exception {
        NewComment newComment = new NewComment("tes".repeat(667));

        String requestContent = mapper.writeValueAsString(newComment);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/10/comments/events/20")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        Mockito.verify(commentService, Mockito.never()).create(
                Mockito.eq(10L), Mockito.eq(20L), Mockito.eq(newComment));
    }

    @Test
    void createShouldThrowBadRequestWhenTextIsEmpty() throws Exception {
        NewComment newComment = new NewComment("  ".repeat(100));

        String requestContent = mapper.writeValueAsString(newComment);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/10/comments/events/20")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        Mockito.verify(commentService, Mockito.never()).create(
                Mockito.eq(10L), Mockito.eq(20L), Mockito.eq(newComment));
    }

    @Test
    void createShouldThrowBadRequestWhenTextIsNull() throws Exception {
        NewComment newComment = new NewComment(null);

        String requestContent = mapper.writeValueAsString(newComment);

        mockMvc.perform(MockMvcRequestBuilders.post("/users/10/comments/events/20")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        Mockito.verify(commentService, Mockito.never()).create(
                Mockito.eq(10L), Mockito.eq(20L), Mockito.eq(newComment));
    }

    @Test
    void updateShouldUpdateCommentCorrectly() throws Exception {
        NewComment newComment = new NewComment("test text".repeat(20));
        String dateTime = LocalDateTime.now().format(customFormatter);
        CommentDto expectedComment = CommentDto.builder()
                .id(1L)
                .text("test text".repeat(20))
                .user(10L)
                .event(20L)
                .createdOn(dateTime)
                .build();

        Mockito.when(commentService.update(10L, 1L, newComment)).thenReturn(expectedComment);

        String requestContent = mapper.writeValueAsString(newComment);

        mockMvc.perform(MockMvcRequestBuilders.patch("/users/10/comments/1")
                        .content(requestContent)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.text").value("test text".repeat(20)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.user").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.event").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.createdOn").value(dateTime));

        Mockito.verify(commentService, Mockito.times(1)).update(
                Mockito.eq(10L), Mockito.eq(1L), Mockito.eq(newComment));
    }

    @Test
    void getByIdShouldReturnCommentCorrectly() throws Exception {
        String dateTime = LocalDateTime.now().format(customFormatter);
        CommentDto expectedComment = CommentDto.builder()
                .id(1L)
                .text("test text".repeat(20))
                .user(10L)
                .event(20L)
                .createdOn(dateTime)
                .build();

        Mockito.when(commentService.getByUserIdAndId(10L, 1L)).thenReturn(expectedComment);

        mockMvc.perform(MockMvcRequestBuilders.get("/users/10/comments/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.text").value("test text".repeat(20)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.user").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.event").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.createdOn").value(dateTime));

        Mockito.verify(commentService, Mockito.times(1)).getByUserIdAndId(10L, 1L);
    }

    @Test
    void deleteShouldDeleteCommentCorrectly() throws Exception {

        Mockito.doNothing().when(commentService).delete(10L, 1L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/users/10/comments/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.content().string(""));

        Mockito.verify(commentService, Mockito.times(1)).delete(
                Mockito.eq(10L), Mockito.eq(1L));
    }

    @Test
    void getAllShouldReturnListCorrectly() throws Exception {
        String dateTime = LocalDateTime.now().format(customFormatter);
        CommentDto firstComment = CommentDto.builder()
                .id(1L)
                .text("firstComment".repeat(20))
                .user(10L)
                .event(20L)
                .createdOn(dateTime)
                .build();
        CommentDto secondComment = CommentDto.builder()
                .id(2L)
                .text("secondComment".repeat(20))
                .user(10L)
                .event(21L)
                .createdOn(dateTime)
                .build();

        List<CommentDto> content = List.of(firstComment, secondComment);
        Pageable expectedPageable = PageRequest.of(0 / 10, 10);
        Page<CommentDto> expectedPage = new PageImpl<>(content, expectedPageable, 1L);

        Mockito.when(commentService.getAll(10L, expectedPageable)).thenReturn(expectedPage);

        mockMvc.perform(MockMvcRequestBuilders.get("/users/10/comments"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].text").value("firstComment".repeat(20)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].user").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].event").value(20))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].createdOn").value(dateTime))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1].id").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1].text").value("secondComment".repeat(20)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1].user").value(10))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1].event").value(21))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1].createdOn").value(dateTime));

        Mockito.verify(commentService, Mockito.times(1)).getAll(Mockito.eq(10L),
                Mockito.eq(expectedPageable));
    }

    @Test
    void getAllShouldShouldThrowBadRequestWhenFromIncorrect() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users/10/comments")
                        .param("from", "-1"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        Mockito.verify(commentService, Mockito.never()).getAll(Mockito.eq(10L), Mockito.any(Pageable.class));
    }

    @Test
    void getAllShouldShouldThrowBadRequestWhenSizeIncorrect() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users/10/comments")
                        .param("size", "0"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));

        Mockito.verify(commentService, Mockito.never()).getAll(Mockito.eq(10L), Mockito.any(Pageable.class));
    }
}
