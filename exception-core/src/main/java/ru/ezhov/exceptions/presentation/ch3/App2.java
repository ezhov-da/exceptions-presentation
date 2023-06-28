package ru.ezhov.exceptions.presentation.ch3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class App2 {
    interface BookRepository {
        List<String> all() throws BookRepositoryException;

        String bookById(String id) throws BookRepositoryException;
    }

    private static class BookRepositoryException extends Exception {
        public BookRepositoryException(String message, Exception cause) {
            super(message, cause);
        }
    }

    private static class BookServiceException extends Exception {
        public BookServiceException(String message, Exception cause) {
            super(message, cause);
        }
    }

    /**
     * Давайте добавим в наш код хранилища метод получения книги по ID и в сервис соответственно.
     *
     * @see BookRepository
     * <p>
     * Мы видим, что так как у нас появился входной параметр, было бы неплохо знать при
     * получении какой именно книги произошла ошибка. Добавим идентификатор в сообщение об ошибке.
     * <p>
     * Слышу возражения о том, что при подходе с непроверяемыми исключениями мы бы так же добавили идентификатор в сообщение.
     * Всё верно, но давайте представим, что уровень сервиса у нас не настолько простой,
     * что в нём есть более сложная логика и входные параметры не являются идентификатором книги.
     * <p>
     * В таком случае из-за неконтролируемого возбуждения ошибки вся информация о входных параметрах уровня сервиса
     * теряется и в логах мы увидим только ID книги, который может быть не самым важным для анализа проблемы
     * или ошибки в сценарии.
     *
     * @see App3
     */
    public static void main(String[] args) {
        try {
            BookService service = new BookService(new DbBookRepository(), new JacksonBookRepository());
            System.out.println(service.all());
            System.out.println(service.bookById("123"));
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

        String bookById(String id) throws BookServiceException {
            try {
                try {
                    return primaryBookRepository.bookById(id);
                } catch (BookRepositoryException ex) {
                    return secondaryBookRepository.bookById(id);
                }
            } catch (BookRepositoryException ex) {
                throw new BookServiceException("Error when get book by " + id, ex);
            }
        }
    }

    private static class JacksonBookRepository implements BookRepository {
        private final String rawBooks = "[\"Book 1\", \"Book 2\"";

        @Override
        public List<String> all() throws BookRepositoryException {
            try {
                return new ObjectMapper().readValue(rawBooks, new TypeReference<>() {
                });
            } catch (JsonProcessingException e) {
                throw new BookRepositoryException("Error when get books", e);
            }
        }

        @Override
        public String bookById(String id) throws BookRepositoryException {
            try {
                return new ObjectMapper()
                        .readValue(rawBooks, new TypeReference<List<String>>() {
                        })
                        .stream()
                        .filter(b -> b.equals(id))
                        .findFirst().get();
            } catch (JsonProcessingException e) {
                throw new BookRepositoryException("Error when get book by " + id, e);
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

        @Override
        public String bookById(String id) throws BookRepositoryException {
            try {
                PreparedStatement ps = DriverManager.getConnection("connection")
                        .prepareStatement("SELECT NAME FROM BOOK WHERE ID = ?");
                ps.setString(1, id);
                ps.executeQuery();
                // здесь обработка
                return "DDD";
            } catch (SQLException e) {
                throw new BookRepositoryException("Error when get book by " + id, e);
            }
        }
    }
}