package com.github.lukasriedler.realworld.spring.handlers

import com.github.lukasriedler.realworld.spring.JwtGenerator
import com.github.lukasriedler.realworld.spring.dtos.*
import com.github.lukasriedler.realworld.spring.endpoints.createNotFoundDto
import com.github.lukasriedler.realworld.spring.endpoints.getEmail
import com.github.lukasriedler.realworld.spring.endpoints.getEmailOrNull
import com.github.lukasriedler.realworld.spring.repositories.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.server.ResponseStatusException

@Component
class CommentHandler(
    private val userRepository: UserRepository,
    private val articleRepository: ArticleRepository,
    private val jwtGenerator: JwtGenerator
) {

    suspend fun getComments(request: ServerRequest): ServerResponse {
        val email = request.getEmailOrNull(jwtGenerator)
        val slug = request.pathVariable("slug")
        val user = if (email != null) {
            userRepository.getUserByEmail(email)
        } else {
            null
        }
        val comments = articleRepository.getComments(slug) ?: return createNotFoundDto()
        return ServerResponse.ok().bodyValueAndAwait(comments.toDto(user))
    }

    suspend fun addComment(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val slug = request.pathVariable("slug")
        val user =
            userRepository.getUserByEmail(email) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        val createCommentDto = request.awaitBody<CreateCommentWrapperDto>()
        val createComment = AddComment(
            body = createCommentDto.comment.body
        )
        val comment = articleRepository.addComment(slug, createComment, user.userId) ?: return createNotFoundDto()
        return ServerResponse.ok().bodyValueAndAwait(CommentWrapperDto(comment.toDto(user)))
    }

    suspend fun deleteComment(request: ServerRequest): ServerResponse {
        val slug = request.pathVariable("slug")
        val commentId = CommentId(request.pathVariable("id").toInt())
        return if (articleRepository.deleteComment(slug, commentId)) {
            ServerResponse.noContent().buildAndAwait()
        } else {
            createNotFoundDto()
        }
    }

    private fun getAuthor(authorUserId: UserId, user: User?): ProfileDto {
        val authorUser =
            userRepository.getUser(authorUserId) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        return authorUser.toProfileDto(user?.following?.contains(authorUserId) ?: false)
    }

    private fun List<Comment>.toDto(user: User?): CommentsDto {
        val comments = this.map {
            it.toDto(user)
        }.toList()
        return CommentsDto(
            comments = comments
        )
    }

    private fun Comment.toDto(user: User?): CommentDto {
        return CommentDto(
            id = this.commentId.id,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            body = this.body,
            author = getAuthor(this.authorUserId, user)
        )
    }
}
