package ru.yandex.practicum.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.user.dto.NewUserRequest;
import ru.yandex.practicum.user.dto.UserDto;
import ru.yandex.practicum.user.exception.ConflictDataException;
import ru.yandex.practicum.user.exception.NotFoundException;
import ru.yandex.practicum.user.model.UserMapper;
import ru.yandex.practicum.user.model.User;
import ru.yandex.practicum.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository repository;

    @Override
    @Transactional
    public UserDto create(NewUserRequest newUserRequest) {
        if (findAnotherUserByEmail(newUserRequest.getEmail()).isPresent()) {
            throw new ConflictDataException("Пользователь с таким email уже существует.");
        }
        User user = UserMapper.mapToNewUser(newUserRequest);
        user = repository.save(user);
        log.debug("Сохранен новый пользователь с id = {}", user.getId());
        return UserMapper.mapToDto(user);
    }

    @Override
    @Transactional
    public void delete(long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", id)));
        repository.delete(user);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        if (ids == null) {
            PageRequest page = PageRequest.of(from > 0 ? from / size : 0, size);
            return repository.findAll(page).stream()
                    .map(UserMapper::mapToDto)
                    .toList();
        } else {
            return repository.findByIds(ids).stream()
                    .map(UserMapper::mapToDto)
                    .toList();
        }
    }

    @Override
    public UserDto getById(Long userId) {
        return repository.findById(userId).map(UserMapper::mapToDto)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", userId)));
    }

    private Optional<User> findAnotherUserByEmail(String email) {
        User existingUser = repository.findByEmail(email);
        return existingUser != null ? Optional.of(existingUser) : Optional.empty();
    }
}
