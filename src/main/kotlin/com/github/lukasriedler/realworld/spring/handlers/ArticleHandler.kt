package com.github.lukasriedler.realworld.spring.handlers

import com.github.lukasriedler.realworld.spring.JwtGenerator
import com.github.lukasriedler.realworld.spring.dtos.*
import com.github.lukasriedler.realworld.spring.endpoints.createErrorDto
import com.github.lukasriedler.realworld.spring.endpoints.createNotFoundDto
import com.github.lukasriedler.realworld.spring.endpoints.getEmail
import com.github.lukasriedler.realworld.spring.endpoints.getEmailOrNull
import com.github.lukasriedler.realworld.spring.repositories.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.server.ResponseStatusException

@Component
class ArticleHandler(
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository,
    private val jwtGenerator: JwtGenerator
) {

    suspend fun getArticles(request: ServerRequest): ServerResponse {
        val email = request.getEmailOrNull(jwtGenerator)
        val user = if (email != null) {
            userRepository.getUserByEmail(email)
        } else {
            null
        }
        val filterTag = request.queryParamOrNull("tag")
        val filterAuthor = request.queryParamOrNull("author")
        val filterAuthorUserId = if (filterAuthor != null) {
            val filterAuthorUser = userRepository.getUserByEmail(filterAuthor)
                ?: return ServerResponse.ok().bodyValueAndAwait(ArticlesDto(listOf(), 0))
            filterAuthorUser.userId
        } else {
            null
        }
        val filterFavorited = request.queryParamOrNull("favorited")
        val filterFavoritedUserId = if (filterFavorited != null) {
            val filterFavoritedUser = userRepository.getUserByUsername(filterFavorited)
                ?: return ServerResponse.ok().bodyValueAndAwait(ArticlesDto(listOf(), 0))
            filterFavoritedUser.userId
        } else {
            null
        }
        val filterLimit = request.queryParamOrNull("limit")?.toIntOrNull() ?: 20
        val filterOffset = request.queryParamOrNull("offset")?.toIntOrNull() ?: 0
        val articles =
            articleRepository.getArticles(
                filterTag,
                filterAuthorUserId,
                filterFavoritedUserId,
                filterLimit,
                filterOffset
            )
        return ServerResponse.ok().bodyValueAndAwait(articles.toDto(user, userRepository))
    }

    suspend fun getArticle(request: ServerRequest): ServerResponse {
        val email = request.getEmailOrNull(jwtGenerator)
        val slug = request.pathVariable("slug")
        val article = articleRepository.getArticle(slug) ?: return createNotFoundDto()
        val author = getAuthor(email, article)
        val favorited = isFavorited(email, article)
        return ServerResponse.ok().bodyValueAndAwait(ArticleWrapperDto(article.toDto(favorited, author)))
    }

    suspend fun deleteArticle(request: ServerRequest): ServerResponse {
        val slug = request.pathVariable("slug")
        val deleted = articleRepository.deleteArticle(slug)
        return if (deleted) {
            ServerResponse.noContent().buildAndAwait()
        } else {
            createNotFoundDto()
        }
    }

    suspend fun createArticle(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val createArticleDto = request.awaitBody<CreateArticleWrapperDto>().article
        val createArticle = CreateArticle(
            title = createArticleDto.title,
            description = createArticleDto.description,
            body = createArticleDto.body,
            tagList = createArticleDto.tagList
        )
        val user =
            userRepository.getUserByEmail(email) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        val article = articleRepository.createArticle(createArticle, user.userId)
            ?: return createErrorDto(listOf("slug already exists"))
        val author = user.toProfileDto(false)
        return ServerResponse.ok().bodyValueAndAwait(ArticleWrapperDto(article.toDto(false, author)))
    }

    suspend fun updateArticle(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val slug = request.pathVariable("slug")
        if (articleRepository.getArticle(slug) == null) {
            return createNotFoundDto()
        }
        val updateArticleDto = request.awaitBody<UpdateArticleWrapperDto>().article
        val updateArticle = UpdateArticle(
            title = updateArticleDto.title,
            description = updateArticleDto.description,
            body = updateArticleDto.body
        )
        val article = articleRepository.updateArticle(slug, updateArticle, email)
            ?: return createErrorDto(listOf("slug already exists"))
        val user =
            userRepository.getUserByEmail(email) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        val author = user.toProfileDto(false)
        return ServerResponse.ok().bodyValueAndAwait(ArticleWrapperDto(article.toDto(false, author)))
    }

    suspend fun getFeed(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val user =
            userRepository.getUserByEmail(email) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        val followedUsers = user.following.toList()
        val filterLimit = request.queryParamOrNull("limit")?.toIntOrNull() ?: 20
        val filterOffset = request.queryParamOrNull("offset")?.toIntOrNull() ?: 0
        val articles = articleRepository.getArticles(
            null,
            null,
            null,
            filterLimit,
            filterOffset
        ).filter { followedUsers.contains(it.authorUserId) }
            .toDto(user, userRepository)
        return ServerResponse.ok().bodyValueAndAwait(articles)
    }

    suspend fun favoriteArticle(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val slug = request.pathVariable("slug")
        val user =
            userRepository.getUserByEmail(email) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        val article = articleRepository.getArticle(slug) ?: return createNotFoundDto()
        article.favoritedBy.add(user.userId)
        return ServerResponse.ok()
            .bodyValueAndAwait(ArticleWrapperDto(article.toDto(isFavorited(email, article), getAuthor(email, article))))
    }

    suspend fun unfavoriteArticle(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val slug = request.pathVariable("slug")
        val user =
            userRepository.getUserByEmail(email) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        val article = articleRepository.getArticle(slug) ?: return createNotFoundDto()
        article.favoritedBy.remove(user.userId)
        return ServerResponse.ok()
            .bodyValueAndAwait(ArticleWrapperDto(article.toDto(isFavorited(email, article), getAuthor(email, article))))
    }

    suspend fun getTags(request: ServerRequest): ServerResponse {
        val tags = articleRepository.getTags()
        return ServerResponse.ok().bodyValueAndAwait(TagsDto(tags = tags))
    }

    private fun getAuthor(email: String?, article: Article): ProfileDto {
        val authorUser = userRepository.getUser(article.authorUserId)
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        val following = if (email != null) {
            val user = userRepository.getUserByEmail(email)
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
            user.following.contains(authorUser.userId)
        } else {
            false
        }
        return authorUser.toProfileDto(following)
    }

    private fun isFavorited(email: String?, article: Article): Boolean {
        return if (email != null) {
            val user = userRepository.getUserByEmail(email)
                ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
            article.favoritedBy.contains(user.userId)
        } else {
            false
        }
    }
}

private fun List<Article>.toDto(user: User?, userRepository: UserRepository): ArticlesDto {
    val articles = this.map {
        val authorUser = userRepository.getUser(it.authorUserId) ?: throw IllegalStateException("missing author")
        it.toDto(
            it.favoritedBy.contains(user?.userId),
            authorUser.toProfileDto(user?.following?.contains(authorUser.userId) ?: false)
        )
    }.toList()
    return ArticlesDto(
        articles = articles,
        articlesCount = articles.size
    )
}

private fun Article.toDto(favorited: Boolean, author: ProfileDto): ArticleDto {
    return ArticleDto(
        slug = this.slug,
        title = this.title,
        description = this.description,
        body = this.body,
        tagList = this.tagList.sorted(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        favorited = favorited,
        favoritesCount = this.favoritedBy.size,
        author = author
    )
}

fun User.toProfileDto(following: Boolean): ProfileDto {
    return ProfileDto(
        username = this.username,
        bio = this.bio,
        image = this.image,
        following = following
    )
}
