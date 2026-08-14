package ru.practicum.explorewithme.controller.comment;

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
import ru.practicum.explorewithme.dto.comment.CommentDto;
import ru.practicum.explorewithme.service.comment.CommentService;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentPublicController.class)
class CommentPublicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Test
    void getByIdShouldReturnCommentCorrectly() throws Exception {
        CommentDto expectedComment = CommentDto.builder()
                .id(10L)
                .text("test comment".repeat(20))
                .event(20L)
                .user(30L)
                .createdOn("2026-03-22 12:00:00")
                .build();

        Mockito.when(commentService.getById(10L)).thenReturn(expectedComment);

        mockMvc.perform(MockMvcRequestBuilders.get("/comments/10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.text").value("test comment".repeat(20)))
                .andExpect(jsonPath("$.event").value(20))
                .andExpect(jsonPath("$.user").value(30))
                .andExpect(jsonPath("$.createdOn").value("2026-03-22 12:00:00"));

        Mockito.verify(commentService, Mockito.times(1)).getById(Mockito.eq(10L));
    }

    @Test
    void getAllByEventShouldReturnCommentsCorrectly() throws Exception {
        CommentDto firstComment = CommentDto.builder()
                .id(10L)
                .text("first comment".repeat(20))
                .event(20L)
                .user(30L)
                .createdOn("2026-03-22 12:00:00")
                .build();
        CommentDto secondComment = CommentDto.builder()
                .id(11L)
                .text("second comment".repeat(20))
                .event(20L)
                .user(31L)
                .createdOn("2026-03-22 12:10:00")
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<CommentDto> expectedPage = new PageImpl<>(List.of(firstComment, secondComment), pageable, 2);

        Mockito.when(commentService.getAllByEvent(20L, pageable)).thenReturn(expectedPage);

        mockMvc.perform(MockMvcRequestBuilders.get("/comments/events/20")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].text").value("first comment".repeat(20)))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].text").value("second comment".repeat(20)));

        Mockito.verify(commentService, Mockito.times(1)).getAllByEvent(Mockito.eq(20L), Mockito.eq(pageable));
    }
}
