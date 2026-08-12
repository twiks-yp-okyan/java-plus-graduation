package ru.practicum.explorewithme.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.dto.user.NewUserRequest;
import ru.practicum.explorewithme.dto.user.UserDto;
import ru.practicum.explorewithme.exception.ConflictDataException;
import ru.practicum.explorewithme.exception.NotFoundException;
import ru.practicum.explorewithme.mapper.UserMapper;
import ru.practicum.explorewithme.model.user.User;
import ru.practicum.explorewithme.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
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
        return UserMapper.mapToDto(user);
    }

    @Override
    @Transactional
    public void delete(long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", id)));
        repository.delete(user);
    }

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
    public boolean userExists(Long userId) {
        return repository.existsById(userId);
    }

    public User getEntityById(long userId) {
        return repository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", userId)));
    }

    private Optional<User> findAnotherUserByEmail(String email) {
        User existingUser = repository.findByEmail(email);
        return existingUser != null ? Optional.of(existingUser) : Optional.empty();
    }
}
