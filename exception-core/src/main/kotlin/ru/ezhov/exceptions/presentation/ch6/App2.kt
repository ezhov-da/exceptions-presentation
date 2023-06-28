package ru.ezhov.exceptions.presentation.ch6

import arrow.core.Either
import arrow.core.getOrElse
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.sql.DriverManager
import java.sql.SQLException

/**
 * @see JacksonBookRepository
 * @see DbBookRepository
 * @see BookService
 * @see main
 *
 * Из плюсов:
 * - Проявленное исключение в контракте
 * - Отсутствие конструкции try-catch в бизнес логике
 *
 * Из минусов:
 * - Новый подход и библиотека
 *
 * @see презентация
 */

fun main(args: Array<String>) {
    val service = BookService(DbBookRepository(), JacksonBookRepository())
    println(service.all().getOrElse { it.printStackTrace(); emptyList() })
    println(service.bookById("123").getOrElse { it.printStackTrace(); null })
}

private class BookRepositoryException(message: String, cause: Exception) : Exception(message, cause)

private class BookServiceException(message: String, cause: Exception) : Exception(message, cause)

private interface BookRepository {
    fun all(): Either<BookRepositoryException, List<String>>
    fun bookById(id: String): Either<BookRepositoryException, String?>
}

private class BookService(
    private val primaryBookRepository: BookRepository,
    private val secondaryBookRepository: BookRepository
) {
    fun all(): Either<BookServiceException, List<String>> =
        primaryBookRepository.all()
            .onRight { secondaryBookRepository.all() }
            .mapLeft { BookServiceException("Error from service when get books", it) }

    fun bookById(id: String): Either<BookServiceException, String?> =
        primaryBookRepository.bookById(id)
            .onRight { secondaryBookRepository.bookById(id) }
            .mapLeft { BookServiceException("Error from service when get book", it) }
}

private class JacksonBookRepository : BookRepository {
    private val rawBooks = "[\"Book 1\", \"Book 2\""
    override fun all(): Either<BookRepositoryException, List<String>> =
        try {
            Either.Right(
                ObjectMapper().readValue(
                    rawBooks,
                    object : TypeReference<List<String>>() {})
            )
        } catch (e: JsonProcessingException) {
            Either.Left(BookRepositoryException("Error when get books", e))
        }

    override fun bookById(id: String): Either<BookRepositoryException, String?> =
        try {
            Either.Right(
                ObjectMapper().readValue(
                    rawBooks,
                    object : TypeReference<List<String?>?>() {})
                    ?.firstOrNull { it == id }
            )
        } catch (e: JsonProcessingException) {
            Either.Left(BookRepositoryException("Error when get book", e))
        }
}

private class DbBookRepository : BookRepository {
    override fun all(): Either<BookRepositoryException, List<String>> =
        try {
            DriverManager.getConnection("connection")
                .createStatement().executeQuery("SELECT NAME FROM BOOK")
            // здесь обработка
            Either.Right(emptyList())
        } catch (e: SQLException) {
            Either.Left(BookRepositoryException("Error when get books", e))
        }


    override fun bookById(id: String): Either<BookRepositoryException, String?> =
        try {
            val ps = DriverManager.getConnection("connection")
                .prepareStatement("SELECT NAME FROM BOOK WHERE ID = ?")
            ps.setString(1, id)
            ps.executeQuery()
            // здесь обработка
            Either.Right("DDD")
        } catch (e: SQLException) {
            Either.Left(BookRepositoryException("Error when get book", e))
        }
}
