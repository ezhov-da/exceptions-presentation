package ru.ezhov.exceptions.presentation.ch4;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Роль исключений в архитектуре приложения
 */
public class App1 {
    interface BookRepository {
        List<String> all() throws BookRepositoryException;

        String bookById(String id) throws BookRepositoryException;
    }

    private static class ClientException extends Exception {
        private final String clientMessage;

        public ClientException(String message, String clientMessage, Throwable cause) {
            super(message, cause);
            this.clientMessage = clientMessage;
        }

        public String getClientMessage() {
            return clientMessage;
        }
    }

    private static class BookRepositoryException extends ClientException {
        public BookRepositoryException(String message, String clientMessage, Throwable cause) {
            super(message, clientMessage, cause);
        }
    }

    private static class BookServiceException extends ClientException {
        public BookServiceException(String message, String clientMessage, Throwable cause) {
            super(message, clientMessage, cause);
        }
    }

    /**
     * Какие выгоды даёт проверяемое исключение для архитектуры?
     * <p>
     * Как мы увидели ранее нам "пришлось" для каждого уровня "хранилище" -> "сервис" описать своё исключение.
     * <p>
     * Это позволило нам:
     * 1. разделить зоны ответственности и дообогатить ошибку на каждом уровне дополнительной информацией
     * 2. заложить возможность не нарушать принцип Лисков при создании других реализаций наших интерфейсов.
     * <p>
     * Разделение исключений по слоям позволит вводить новые исключения и
     * реагировать на них в случае необходимости на уровне представления.
     *
     * Презентация
     * @see ru.ezhov.exceptions.presentation.ch5.App1
     */
    public static void main(String[] args) {
        try {
            BookService service = new BookService(new DbBookRepository(), new JacksonBookRepository());
            System.out.println(service.all());
            System.out.println(service.bookById("123"));
        } catch (BookServiceException ex) {
            ex.printStackTrace();
            System.err.println(ex.getClientMessage());
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
                throw new BookServiceException("Error when get books", "Error when get books, please try later", ex);
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
                throw new BookServiceException("Error when get book by " + id, "Error when get book, please try later", ex);
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
                throw new BookRepositoryException("Error when get books", "Error when get books, please try later", e);
            }
        }

        @Override
        public String bookById(String id) throws BookRepositoryException {
            try {
                return new ObjectMapper().readValue(rawBooks, new TypeReference<List<String>>() {
                }).stream().filter(b -> b.equals(id)).findFirst().get();
            } catch (JsonProcessingException e) {
                throw new BookRepositoryException(
                        "Error when get book by " + id,
                        "Error when get book, please try later",
                        e
                );
            }
        }
    }

    private static class DbBookRepository implements BookRepository {
        @Override
        public List<String> all() throws BookRepositoryException {
            try {
                DriverManager.getConnection("connection")
                        .createStatement().executeQuery("SELECT NAME FROM BOOK");
                // здесь обработка
                return new ArrayList<>();
            } catch (SQLException e) {
                throw new BookRepositoryException(
                        "Error when get books",
                        "Error when get books, please try later",
                        e
                );
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
                throw new BookRepositoryException(
                        "Error when get book by " + id,
                        "Error when get book, please try later",
                        e
                );
            }
        }
    }
}