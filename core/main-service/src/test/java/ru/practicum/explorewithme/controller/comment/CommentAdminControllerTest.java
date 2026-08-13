package ru.practicum.explorewithme.controller.comment;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.practicum.explorewithme.service.comment.CommentService;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentAdminController.class)
class CommentAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Test
    void deleteShouldDeleteCommentCorrectly() throws Exception {
        Mockito.doNothing().when(commentService).deleteByAdmin(10L);

        mockMvc.perform(MockMvcRequestBuilders.delete("/admin/comments/10"))
                .andExpect(status().isOk());

        Mockito.verify(commentService, Mockito.times(1)).deleteByAdmin(Mockito.eq(10L));
    }
}
