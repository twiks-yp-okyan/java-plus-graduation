package ru.yandex.practicum.user.service;

import ru.yandex.practicum.user.dto.NewUserRequest;
import ru.yandex.practicum.user.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto create(NewUserRequest newUserRequest);

    void delete(long id);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    UserDto getById(Long userId);
}
