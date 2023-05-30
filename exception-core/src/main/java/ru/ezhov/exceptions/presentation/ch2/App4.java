package ru.ezhov.exceptions.presentation.ch2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Что мы имеем на данный момент?
 * <p>
 * 1. 2-е из 2-х реализаций обязывают нас обрабатывать проверяемые исключения
 * 2. Обработка этих исключений (в какое оборачивать и прочее) зависит от договорённости в команде (хорошо, если договорённость есть)
 * 3. Приложение ничего не знает о том, что получение книг может выдавать ошибку
 * <p>
 * Представим, что получение книг - это одна из критичных функций нашего приложения и от бизнеса поступает запрос:
 * В случае ошибки получения книг, нам нужно попытаться получить их из другого хранилища.
 * <p>
 * Что мы можем сделать с нашим кодом и реализациями?
 * <p>
 * Всё верно, в сервисе принимать несколько хранилищ и в случае ошибки
 * попробовать получить данные из другого хранилища.
 */
public class App4 {
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
         * Как же нам поступить при обработке? Ведь мы не знаем на какое именно исключение реагировать.
         * Наверно:
         * 1. Можем посмотреть в реализацию метода, но ни кто не гарантирует что это исключение будет всегда.
         * 2. Можем обернуть всё в Exception, но тогда и разные NullPointerException тоже будем ловить,
         * а нам важна только доступность хранилища и возможность получить из него данные.
         * 3. Может есть и третий вариант?
         * <p>
         * Взвесив все за и против, приняв риски я выбираю второй вариант.
         * <p>
         * Казалось бы - всё хорошо, но в данной ситуации мы:
         * 1. Отлавливаем все исключения
         * 2. В случае, если каждое из хранилищ выдаёт своё исключение,
         * метод нашего сервиса неявно получает возможность возбуждать разные исключения опосредованно,
         * что в свою очередь затрудняет диагностику и профилактику ошибок.
         *
         * Что же нам можно сделать?
         * Давайте вернёмся к нашим реализациям и посмотрим, что у них общего.
         */
        List<String> all() {
            try {
                return primaryBookRepository.all();
            } catch (Exception ex) {
                return secondaryBookRepository.all(); // Здесь вылетит какое-то исключение из реализации
            }
        }
    }

    /**
     * Что общего у реализаций:
     * 1. Оба класса реализуют интерфейс BookRepository
     * 2. В обеих реализациях может произойти ошибка
     * 3. Оба класса неявно прокидывают информацию об ошибке дальше
     *
     * Все три пункта можно объединить одним словом - контракт.
     * Что такое контракт в программировании?
     *
     * https://ru.wikipedia.org/wiki/%D0%9A%D0%BE%D0%BD%D1%82%D1%80%D0%B0%D0%BA%D1%82%D0%BD%D0%BE%D0%B5_%D0%BF%D1%80%D0%BE%D0%B3%D1%80%D0%B0%D0%BC%D0%BC%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5
     * Контрактное программирование — это метод проектирования программного обеспечения.
     * Он предполагает, что проектировщик должен определить формальные, точные и верифицируемые спецификации
     * интерфейсов для компонентов системы.
     * При этом, кроме обычного определения абстрактных типов данных, также используются предусловия,
     * постусловия и инварианты.
     *
     * Что же, как завещает один из языков программирования - "Явное лучше, чем неявное" :)
     * Давайте сделаем наш контракт явным
     * @see App5
     */
    private interface BookRepository {
        List<String> all();
    }

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