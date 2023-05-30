package ru.ezhov.exceptions.presentation.ch2;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Итак, книги у нас хранятся в БД, сейчас не важно в какой именно.
 * Используем JDBC для их получения.
 */
public class App3 {
    public static void main(String[] args) {
        BookService service = new BookService(new DbBookRepository());

        System.out.println(service.all());
    }

    private static class BookService {
        private final BookRepository bookRepository;

        public BookService(BookRepository bookRepository) {
            this.bookRepository = bookRepository;
        }

        List<String> all() {
            return bookRepository.all();
        }
    }

    private interface BookRepository {
        List<String> all();
    }

    /**
     * Реализуем хранилище и видим, что появляется другое проверяемое исключение
     *
     * @see SQLException
     * <p>
     * Как мы поступим?
     * <p>
     * Верно, так же как и в предыдущий раз, тем более, что IDEA из коробки предлагает такой вариант.
     *
     * @see App4
     */
    private static class DbBookRepository implements BookRepository {
        @Override
        public List<String> all() {
            try {
                DriverManager.getConnection("connection")
                        .createStatement()
                        .executeQuery("SELECT NAME FROM BOOK");
                // здесь обработка
                return new ArrayList<>();
            } catch (SQLException e) {
                throw new RuntimeException("Error when get books", e);
            }
        }
    }
}