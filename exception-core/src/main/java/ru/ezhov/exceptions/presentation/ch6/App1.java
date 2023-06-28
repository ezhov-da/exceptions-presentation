package ru.ezhov.exceptions.presentation.ch6;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Есть ли жизнь без исключений?
 * <p>
 * В данном примере мы заменили контракты хранилища и сервиса на использование Try.
 *
 * @see JacksonBookRepository
 * @see BookService
 * @see App1#main
 * <p>
 * Из плюсов:
 * - Контракт явно говорит о том, что может произойти ошибка
 * - Лишились многословных конструкций try-catch в бизнес логике
 * - Обработка исключения необходима только на верхнем уровне
 * <p>
 * Из минусов:
 * - Новый подход и библиотека
 * - Перехват Throwable, а это грозит и перехватом Error
 * - Потеряли возможность понимать какое исключение может прийти и соответственно реагировать на него
 * <p>
 *
 * @see Kotlin
 * @see ru.ezhov.exceptions.presentation.ch6.App2
 */
public class App1 {
    interface BookRepository {
        Try<List<String>> all();

        Try<String> bookById(String id);
    }

    public static void main(String[] args) {
        BookService service = new BookService(new DbBookRepository(), new JacksonBookRepository());
        Try<List<String>> all = service.all();
        if (all.isSuccess()) {
            System.out.println(all.get());
        } else {
            all.getCause().printStackTrace();
        }

        Try<String> book = service.bookById("123");
        if (book.isSuccess()) {
            System.out.println(book.get());
        } else {
            book.getCause().printStackTrace();
        }
    }

    private static class BookService {
        private final BookRepository primaryBookRepository;
        private final BookRepository secondaryBookRepository;

        public BookService(BookRepository primaryBookRepository, BookRepository secondaryBookRepository) {
            this.primaryBookRepository = primaryBookRepository;
            this.secondaryBookRepository = secondaryBookRepository;
        }

        Try<List<String>> all() {
            return primaryBookRepository
                    .all()
                    .onFailure(t -> secondaryBookRepository.all()); // можно обработать ошибку
        }

        Try<String> bookById(String id) {
            return primaryBookRepository.bookById(id)
                    .onFailure(t -> secondaryBookRepository.bookById(id));
        }
    }

    private static class JacksonBookRepository implements BookRepository {
        private final String rawBooks = "[\"Book 1\", \"Book 2\"";

        @Override
        public Try<List<String>> all() {
            return Try.ofSupplier(() ->
                    {
                        try {
                            return new ObjectMapper().readValue(rawBooks, new TypeReference<>() {
                            });
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
        }

        @Override
        public Try<String> bookById(String id) {
            return Try.ofSupplier(() ->
            {
                try {
                    return new ObjectMapper().readValue(rawBooks, new TypeReference<List<String>>() {
                    }).stream().filter(b -> b.equals(id)).findFirst().get();
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static class DbBookRepository implements BookRepository {
        @Override
        public Try<List<String>> all() {
            return Try.ofSupplier(() ->
            {
                try {
                    DriverManager.getConnection("connection")
                            .createStatement().executeQuery("SELECT NAME FROM BOOK");
                    // здесь обработка
                    return new ArrayList<>();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public Try<String> bookById(String id) {
            return Try.ofSupplier(() ->
            {
                try {
                    PreparedStatement ps = DriverManager.getConnection("connection")
                            .prepareStatement("SELECT NAME FROM BOOK WHERE ID = ?");
                    ps.setString(1, id);
                    ps.executeQuery();
                    // здесь обработка
                    return "DDD";
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}