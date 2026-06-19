package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TodoCustomRepository {

    Optional<Todo> findByIdWithUser(Long todoId);

    // 제목, 생성일, 담당자닉네임
    Page<TodoSearchResponse> searchTodos(
        String keyword,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String managerNickname,
        Pageable pageable
    );
}