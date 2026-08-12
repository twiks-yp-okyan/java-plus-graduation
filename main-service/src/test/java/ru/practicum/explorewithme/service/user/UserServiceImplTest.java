package ru.practicum.explorewithme.service.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.explorewithme.dto.user.NewUserRequest;
import ru.practicum.explorewithme.dto.user.UserDto;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.model.user.User;
import ru.practicum.explorewithme.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void create_shouldSaveAndReturnDto_whenEmailIsUnique() {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("test@example.com");
        request.setName("Test User");

        User savedUser = User.builder()
                .id(1L)
                .email(request.getEmail())
                .name(request.getName())
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDto result = userService.create(request);

        assertNotNull(result);
        assertEquals(savedUser.getId(), result.getId());
        assertEquals(savedUser.getEmail(), result.getEmail());
        assertEquals(savedUser.getName(), result.getName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_shouldThrowConflictDataException_whenEmailAlreadyExists() {
        NewUserRequest request = new NewUserRequest();
        request.setEmail("test@example.com");
        request.setName("Test User");

        User existingUser = User.builder()
                .id(1L)
                .email(request.getEmail())
                .name("Existing User")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(existingUser);

        assertThrows(ConflictDataException.class, () -> userService.create(request));
    }

    @Test
    void delete_shouldDeleteUser_whenUserExists() {
        long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.delete(userId);

        verify(userRepository).delete(eq(user));
    }

    @Test
    void delete_shouldThrowNotFoundException_whenUserDoesNotExist() {
        long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.delete(userId));
    }

    @Test
    void getUsers_shouldReturnPagedUsers_whenIdsIsNull() {
        int from = 0;
        int size = 10;
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();

        when(userRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        List<UserDto> result = userService.getUsers(null, from, size);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(user.getId(), result.getFirst().getId());
    }

    @Test
    void getUsers_shouldReturnUsersByIds_whenIdsProvided() {
        List<Long> ids = List.of(1L, 2L);
        User user1 = User.builder()
                .id(1L)
                .email("first@example.com")
                .name("First User")
                .build();
        User user2 = User.builder()
                .id(2L)
                .email("second@example.com")
                .name("Second User")
                .build();

        when(userRepository.findByIds(ids)).thenReturn(List.of(user1, user2));

        List<UserDto> result = userService.getUsers(ids, 0, 10);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(user1.getId(), result.get(0).getId());
        assertEquals(user2.getId(), result.get(1).getId());
    }

    @Test
    void getEntityById_shouldReturnUser_whenUserExists() {
        long userId = 1L;
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getEntityById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void getEntityById_shouldThrowNotFoundException_whenUserDoesNotExist() {
        long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getEntityById(userId));
    }
}

