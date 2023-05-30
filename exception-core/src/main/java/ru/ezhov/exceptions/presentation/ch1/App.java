package ru.ezhov.exceptions.presentation.ch1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        try {
            String fileTextChecked = readFileWithCheckedException();
        } catch (IOException e) {
            // 3 Можно заметить, что при обработке проверяемых исключений необходимо
            // писать больше кода и заботиться об обработке
            System.err.println("Error");
        }

        int divideByZero = divideByZeroWithUncheckedException();
    }

    /**
     * 1 Метод возбуждает проверяемое исключение, при чтении файла, которое программист
     * обязан обработать или прокинуть дальше
     */
    private static String readFileWithCheckedException() throws IOException {
        return new String(Files.readAllBytes(Paths.get("test-file.txt")));
    }

    /**
     * 2 Метод возбуждает непроверяемое исключение, которое программист может не обрабатывать.
     */
    private static int divideByZeroWithUncheckedException() {
        return 1 / 0;
    }
}
