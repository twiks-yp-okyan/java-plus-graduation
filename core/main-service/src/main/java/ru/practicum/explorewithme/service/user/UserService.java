package ru.practicum.explorewithme.service.user;

import ru.practicum.explorewithme.dto.user.NewUserRequest;
import ru.practicum.explorewithme.dto.user.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest newUserRequest);

    void delete(long id);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    boolean userExists(Long userId);
}
