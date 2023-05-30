package ru.ezhov.exceptions.presentation.ch2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Итак, предположим, что книги у нас хранятся в JSON, тогда используем для их чтения Jackson.
 * И реализуем с его использованием хранилище.
 */
public class App2 {
    public static void main(String[] args) {
        BookService service = new BookService(new JacksonBookRepository());

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
     * Мы видим, что Jackson при чтении файла возбуждает сразу два исключения.
     *
     * @see JsonProcessingException
     * @see JsonMappingException
     * <p>
     * И единственнный на данный момент вариант - обернуть эти исключения в непроверяемое исключение.
     * <p>
     * Сразу возникает вопрос, в какое исключение обернуть?
     * <p>
     * Тут всё зависит от договорённости подхода в команде - создать своё или использовать существующее.
     * Я использую существующее.
     * <p>
     * Давайте посмотрим, что будет, если нам придёт невалидный JSON.
     * <p>
     * На самом верхнем уровне мы получаем информацию непосредственно из хранилища о том, что произошла ошибка.
     * <p>
     * Ок, как буд-то всё работает как и ожидается, но что, если нам нужно реализовать получение данных из БД?
     *
     * @see App3
     */
    private static class JacksonBookRepository implements BookRepository {
        private final String rawBooks = "[\"Book 1\", \"Book 2\"";

        @Override
        public List<String> all() {
            try {
                return new ObjectMapper().readValue(rawBooks, List.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error when get books", e);
            }
        }
    }
}