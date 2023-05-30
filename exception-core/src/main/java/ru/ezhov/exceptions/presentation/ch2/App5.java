package ru.ezhov.exceptions.presentation.ch2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class App5 {
    /**
     * Итак, как нам проявить контракт?
     * <p>
     * Очень просто. Нам нужно добавить проверяемое исключение в сигнатуру метода.
     * Для этого
     * 1. создаём своё проверяемое исключение
     *
     * @see BookRepositoryException
     * 2. добавляем его в сигшнатуру метода
     * <p>
     * Давайте посмотрим, как изменилась работа с исключениями в нашем коде.
     */
    interface BookRepository {
        List<String> all() throws BookRepositoryException;
    }

    private static class BookRepositoryException extends Exception {
        public BookRepositoryException(String message, Exception cause) {
            super(message, cause);
        }
    }

    public static void main(String[] args) {
        BookService service = new BookService(new DbBookRepository(), new JacksonBookRepository());

        System.out.println(service.all());
    }

    private static class BookService {
        private final BookRepository primaryBookRepository;
        private final BookRepository secondaryBookRepository;

        public BookService(BookRepository primaryBookRepository, BookRepository secondaryBookRepository) {
            this.primaryBookRepository = primaryBookRepository;
            this.secondaryBookRepository = secondaryBookRepository;
        }

        /**
         * Теперь мы знаем, чего ожидать от хранилищ и можем обработать только необходимые нам исключения.
         * <p>
         * Более того, если нам понадобится использовать или реализовать другую логику получения книг, нам не придётся
         * переживать о новых типах исключениях и "забывчивости" их обработать.
         * <p>
         * Но что же мы видим в коде нашего сервиса?
         * <p>
         * Обёртывание нашего проверяемого исключения в непроверяемое!!!
         * <p>
         * Не является ли этот подход тем же самым, что и при предыдущей обработке непроверяемых исключений в хранилище?
         * Является :(
         * <p>
         * Чем он плох?
         * 1. Отсутствие явного контракта - как следствие сложная обработка исключительных ситуаций на уровнях выше
         * 2. "Многа букафф" для обработки ошибки
         * <p>
         * Чем он хоршо?
         * 1. Ненужно заботиться об ошибках на уровне выше и в случае проблем можно сказать "Кто же знал :)"
         * 2. "Мало букафф" :)
         * <p>
         * Все мы помним, что уровнем выше находится наш уровень представления.
         * Для консольного приложения - это консоль
         * Для WEB приложения - это контроллер
         * <p>
         * Для того, чтоб была выстроена правильная коммуникация с пользователем, нам необходимо дать понять
         * пользователю, что пошло не так.
         * Давайте посмотрим как это будет выглядеть
         *
         * @see App6
         */
        List<String> all() {
            try {
                try {
                    return primaryBookRepository.all();
                } catch (BookRepositoryException ex) {
                    return secondaryBookRepository.all();
                }
            } catch (BookRepositoryException ex) {
                throw new RuntimeException("Error when get books", ex);
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