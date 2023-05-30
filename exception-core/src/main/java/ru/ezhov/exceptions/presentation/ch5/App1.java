package ru.ezhov.exceptions.presentation.ch5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Проверяемые исключения зло!
 */
public class App1 {
    /**
     * В чём же заключается зло?
     * <p>
     * В случае, если нам необходимо выполнить в lambda выражении действие над списком используя
     * метод с проверяемым исключение, нам приходиться жонглировать с обработкой исключений.
     * <p>
     * Как с этим жить?
     * <p>
     * В первую очередь задать себе вопрос: "А действительно ли я делаю то, что надо?"
     * <p>
     * Может мне не хватает какого-то дополнительного метода или мне нужно собрать все ошибки?
     * <p>
     * Презентация "Есть ли жизнь без исключений", затем:
     *
     * @see ru.ezhov.exceptions.presentation.ch6.App1
     */
    public static void main(String[] args) {
        BookRepository repository = new BookRepository();

        //Нехватка метода получения книг по нескольким ID
        Arrays.asList("1", "2", "3", "4").forEach(id -> {
            try {
                repository.book(id);
            } catch (BookRepositoryException e) {
                throw new RuntimeException(e);
            }
        });

        //Необходимость получения всех ошибок
        List<BookRepositoryException> errors = new ArrayList<>();
        Arrays.asList("1", "2", "3", "4").forEach(id -> {
            try {
                repository.book(id);
            } catch (BookRepositoryException e) {
                errors.add(e);
            }
        });
    }

    private static class BookRepositoryException extends Exception {
    }

    private static class BookRepository {
        String book(String id) throws BookRepositoryException {
            return "123";
        }
    }
}
