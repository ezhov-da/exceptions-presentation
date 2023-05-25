package ru.ezhov.exceptions.presentation.ch2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.ezhov.exceptions.presentation.ch3.App2;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class App7 {
    interface BookRepository {
        List<String> all() throws BookRepositoryException;
    }

    private static class BookRepositoryException extends Exception {
        public BookRepositoryException(String message, Exception cause) {
            super(message, cause);
        }
    }

    /**
     * Создадим собственное проверяемое исключение для сервиса и добавим его в сигнатуру метода
     */
    private static class BookServiceException extends Exception {
        public BookServiceException(String message, Exception cause) {
            super(message, cause);
        }
    }

    /**
     * Посмотрим, как изменился наш код
     * 1. В сервисе добавили оборачивание в наше исключение
     * 2. На уровне представления у нас появилось понимание того, что ожидать от кода и как обрабатывать
     * 3. Обратите внимание, что секция перехвата Exception осталась, так как непроверяемые исключения никуда не делись
     * <p>
     * При таком подходе:
     * 1. Можем по разному реагировать на исключения
     * - разный текст сообщения
     * - разный код ответа
     * Обратите внимание на текст об ошибке.
     * В одном случае мы знаем что могла произойти ошибка и залогировать это одним уровнем
     * В другом случае произошла непредвиденная ошибка и, возможно, это другой уровень логированния.
     * 2. Легко расширять код не боясь "забыть" обработать исключения
     * 3. Более простое следование принципу Лисков
     * 4. Добавляется нагрузка на разработчика по идентификации ошибок и их обработке
     * <p>
     * Отдельно хочу подчеркнуть, что гигиена работы с ошибками и исключительными ситуация при работе в команде
     * лежит и на аналитике в том числе. Именно от аналитика зависит цельность контракта и корректная работа с исключениями.
     * Например, если в методе получения данных может произойти ошибка и необходимо пользователя об этом уведомить, именно аналитик фиксирует это в контракте,
     * в противном случае можем получить 500 "Неожиданная ошибка".
     *
     * @see App2
     */
    public static void main(String[] args) {
        try {
            BookService service = new BookService(new DbBookRepository(), new JacksonBookRepository());
            System.out.println(service.all());
        } catch (BookServiceException ex) {
            ex.printStackTrace();
            System.err.println("Error when get all books");
        } catch (Exception ex) {
            System.err.println("An unexpected error occurred when get all books");
            ex.printStackTrace();
        }
    }

    private static class BookService {
        private final BookRepository primaryBookRepository;
        private final BookRepository secondaryBookRepository;

        public BookService(BookRepository primaryBookRepository, BookRepository secondaryBookRepository) {
            this.primaryBookRepository = primaryBookRepository;
            this.secondaryBookRepository = secondaryBookRepository;
        }

        List<String> all() throws BookServiceException {
            try {
                try {
                    return primaryBookRepository.all();
                } catch (BookRepositoryException ex) {
                    return secondaryBookRepository.all();
                }
            } catch (BookRepositoryException ex) {
                throw new BookServiceException("Error when get books", ex);
            }
        }
    }

    private static class JacksonBookRepository implements BookRepository {
        private final String rawBooks = "[\"Book 1\", \"Book 2\"";

        @Override
        public List<String> all() throws BookRepositoryException {
            try {
                return new ObjectMapper().readValue(rawBooks, List.class);
            } catch (JsonProcessingException e) {
                throw new BookRepositoryException("Error when get books", e);
            }
        }
    }

    private static class DbBookRepository implements BookRepository {
        @Override
        public List<String> all() throws BookRepositoryException {
            try {
                DriverManager.getConnection("connection")
                        .createStatement()
                        .executeQuery("SELECT NAME FROM BOOK");
                // здесь обработка
                return new ArrayList<>();
            } catch (SQLException e) {
                throw new BookRepositoryException("Error when get books", e);
            }
        }
    }
}