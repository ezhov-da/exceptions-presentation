package ru.ezhov.exceptions.presentation.ch2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class App6 {
    interface BookRepository {
        List<String> all() throws BookRepositoryException;
    }

    private static class BookRepositoryException extends Exception {
        public BookRepositoryException(String message, Exception cause) {
            super(message, cause);
        }
    }

    /**
     * Исходя из опыта, мы понимаем, что любой сервис может выдать ошибку, но так же исходя из опыта, мы значем,
     * что любую ошибку можно обернуть в Exception.
     * <p>
     * Давайте так и поступим.
     * <p>
     * Что мы видим?
     * 1. Мы застрахованы от любой ошибки
     * 2. Мы застрахованы от знания о том, какая ошибка может быть :)
     * 3. Мы не можем сказать пользователю или сторонней системе какого рода эта ошибка (инфраструктурная, логическая, и т.д.)
     * <p>
     * Что мы можем сделать?
     * <p>
     * Верно, применить такой же подход как и в случае с хранилищем.
     * Давайте попробуем.
     *
     * @see App7
     */
    public static void main(String[] args) {
        try {
            BookService service = new BookService(new DbBookRepository(), new JacksonBookRepository());
            System.out.println(service.all());
        } catch (Exception ex) {
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