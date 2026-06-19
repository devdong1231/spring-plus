package org.example.expert.domain.todo.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.comment.entity.QComment;
import org.example.expert.domain.manager.entity.QManager;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.QTodo;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.user.entity.QUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TodoCustomRepositoryImpl implements TodoCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        QTodo todo = QTodo.todo;
        QUser user = QUser.user;

        Todo result = queryFactory
            .selectFrom(todo)
            .join(todo.user, user).fetchJoin()
            .where(todo.id.eq(todoId))
            .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Page<TodoSearchResponse> searchTodos(
        String keyword,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String managerNickname,
        Pageable pageable
    ) {
        QTodo todo = QTodo.todo;
        QManager managerCount = new QManager("managerCount");
        QManager managerSearch = new QManager("managerSearch");
        QUser managerUser = new QUser("managerUser");
        QComment commentCount = new QComment("commentCount");

        BooleanBuilder conditions = new BooleanBuilder();
        if (StringUtils.hasText(keyword)) {
            conditions.and(todo.title.containsIgnoreCase(keyword));
        }
        if (startDate != null) {
            conditions.and(todo.createdAt.goe(startDate));
        }
        if (endDate != null) {
            conditions.and(todo.createdAt.loe(endDate));
        }
        if (StringUtils.hasText(managerNickname)) {
            conditions.and(
                JPAExpressions
                    .selectOne()
                    .from(managerSearch)
                    .join(managerSearch.user, managerUser)
                    .where(
                        managerSearch.todo.eq(todo),
                        managerUser.nickname.containsIgnoreCase(managerNickname)
                    )
                    .exists()
            );
        }

        List<TodoSearchResponse> content = queryFactory
            .select(Projections.constructor(
                TodoSearchResponse.class,
                todo.title,
                JPAExpressions
                    .select(managerCount.count())
                    .from(managerCount)
                    .where(managerCount.todo.eq(todo)),
                JPAExpressions
                    .select(commentCount.count())
                    .from(commentCount)
                    .where(commentCount.todo.eq(todo))
            ))
            .from(todo)
            .where(conditions)
            .orderBy(todo.createdAt.desc(), todo.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = queryFactory
            .select(todo.count())
            .from(todo)
            .where(conditions)
            .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}