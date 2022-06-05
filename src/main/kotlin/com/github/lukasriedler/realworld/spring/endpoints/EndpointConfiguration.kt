package com.github.lukasriedler.realworld.spring.endpoints

import com.github.lukasriedler.realworld.spring.JwtGenerator
import com.github.lukasriedler.realworld.spring.dtos.ErrorsDto
import com.github.lukasriedler.realworld.spring.dtos.ErrorsWrapperDto
import com.github.lukasriedler.realworld.spring.handlers.ArticleHandler
import com.github.lukasriedler.realworld.spring.handlers.CommentHandler
import com.github.lukasriedler.realworld.spring.handlers.UserHandler
import com.github.lukasriedler.realworld.spring.repositories.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.*
import org.springframework.web.server.ResponseStatusException

@Configuration
class EndpointConfiguration(private val jwtGenerator: JwtGenerator, private val userRepository: UserRepository) {

    @Bean
    fun routes(userHandler: UserHandler, articleHandler: ArticleHandler, commentHandler: CommentHandler) = coRouter {
        // authenticated
        ("/api" and contentType(MediaType.APPLICATION_JSON)).nest {
            before(this@EndpointConfiguration::requireAuth)

            GET("/user", userHandler::getUser)
            PUT("/user", userHandler::updateUser)

            POST("/profiles/{username}/follow", userHandler::followUser)
            DELETE("/profiles/{username}/follow", userHandler::unfollowUser)

            POST("/articles", articleHandler::createArticle)
            GET("/articles/feed", articleHandler::getFeed)
            PUT("/articles/{slug}", articleHandler::updateArticle)
            DELETE("/articles/{slug}", articleHandler::deleteArticle)

            POST("/articles/{slug}/favorite", articleHandler::favoriteArticle)
            DELETE("/articles/{slug}/favorite", articleHandler::unfavoriteArticle)

            POST("/articles/{slug}/comments", commentHandler::addComment)
            DELETE("/articles/{slug}/comments/{id}", commentHandler::deleteComment)
        }

        // unauthenticated
        ("/api" and contentType(MediaType.APPLICATION_JSON)).nest {
            POST("/users", userHandler::createUser)
            POST("/users/login", userHandler::loginUser)

            GET("/profiles/{username}", userHandler::getProfile)

            GET("/articles", articleHandler::getArticles)
            GET("/articles/{slug}", articleHandler::getArticle)

            GET("/articles/{slug}/comments", commentHandler::getComments)

            GET("/tags", articleHandler::getTags)
        }
    }

    fun requireAuth(request: ServerRequest): ServerRequest {
        val token = request.getJWT()
        return if (jwtGenerator.verifyToken(token)) {
            val email =
                jwtGenerator.emailFromToken(token) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
            if (userRepository.getUserByEmail(email) == null) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
            }
            request
        } else {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        }
    }
}

fun ServerRequest.getAuthorization(): String? = this.headers().header("Authorization").firstOrNull()

fun ServerRequest.getJWT(): String {
    val authorization = this.getAuthorization() ?: throw ResponseStatusException(HttpStatus.FORBIDDEN)
    val splitAuthorization = authorization.split(" ")
    if (splitAuthorization.size != 2) {
        throw ResponseStatusException(HttpStatus.FORBIDDEN)
    }
    return splitAuthorization[1]
}

fun ServerRequest.getJWTOrNull(): String? {
    val authorization = this.getAuthorization() ?: return null
    val splitAuthorization = authorization.split(" ")
    if (splitAuthorization.size != 2) {
        return null
    }
    return splitAuthorization[1]
}

fun ServerRequest.getEmailOrNull(jwtGenerator: JwtGenerator): String? {
    val token = this.getJWTOrNull() ?: return null
    return jwtGenerator.emailFromToken(token)
}

fun ServerRequest.getEmail(jwtGenerator: JwtGenerator): String {
    val token = this.getJWT()
    return jwtGenerator.emailFromToken(token) ?: throw ResponseStatusException(HttpStatus.FORBIDDEN)
}

suspend fun createNotFoundDto() = ServerResponse.notFound().buildAndAwait()

suspend fun createErrorDto(body: List<String>) =
    ServerResponse.status(422).bodyValueAndAwait(ErrorsWrapperDto(errors = ErrorsDto(body)))
