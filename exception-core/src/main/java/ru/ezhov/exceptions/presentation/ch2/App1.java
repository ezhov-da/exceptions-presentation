package ru.ezhov.exceptions.presentation.ch2;

import java.util.ArrayList;
import java.util.List;

/**
 * У нас есть приложение, которое получает список книг и выводит их пользователю.
 * Для упрощения кода, будем считать, что метод main - это уровень представления.
 * Тогда нам необходим сервисный уровень и уровень хранилища данных.
 * <p>
 * Уровень хранилища данных представлен интерфейсом
 *
 * @see ru.ezhov.exceptions.presentation.ch2.App1.BookRepository
 */
public class App1 {
    public static void main(String[] args) {
        BookRepository repository = ArrayList::new;

        BookService service = new BookService(repository);

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

    /**
     * Что можно узнать из контракта этого интерфейса?
     * <p>
     * То, что возвращается список (допустим названия книг), а так же то,
     * что можно кинуть абсолютно любое непроверяемое исключение.
     * <p>
     * Ок, если это интерфейс, то можно сделать его реализацию, давайте так и поступим.
     *
     * @see App2
     */
    private interface BookRepository {
        List<String> all();
    }
}