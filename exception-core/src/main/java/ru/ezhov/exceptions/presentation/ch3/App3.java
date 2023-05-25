package ru.ezhov.exceptions.presentation.ch3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class App3 {
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
     * Уведомление пользователю.
     * <p>
     * Как было сказано ранее, исключение - это такой же транспорт как и другие объекты.
     * <p>
     * По-этому у нас есть возможность добавлять в него необходимую нам информацию для анализа
     * проблемы и выводу информации пользователю.
     * <p>
     * Очень часто, я вижу, как на проектах используется для уведомления пользователя поле message из исключения.
     * <p>
     * Какие неприятные последствия это может нести:
     * 1. Раскрытие чувствительных данных (пароли, пин коды, телефоны)
     * 2. Подробности об используемых технологиях
     * <p>
     * И в то же время разработчикам самого приложения эти данные крайне важны для отладки.
     * <p>
     * Что мы можем сделать?
     * <p>
     * Учитывая то, что сообщения внешним пользователям об ошибке в работе приложения и сообщение
     * разработчикам об ошибке в приложении преследуют две разных цели, нам достаточно это проявить в коде,
     * добавив в исключение дополнительное поле.
     * <p>
     * Как изменился наш код?
     * <p>
     * Конечно, указание сообщений для пользователя на уровне хранилища несколько противоречит концепции
     * обязанности уровней приложения. Но ни кто не обязывает указывать сообщение на уровне хранилища, воможно,
     * вам достаточно будет указать его на уровне сервиса, для чего он и предназначен.
     *
     * Как итог - использование собственных исключений с понятным контрактом позволяет уйти от неожиданностей в логах
     * и проявить опасные места в коде.
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