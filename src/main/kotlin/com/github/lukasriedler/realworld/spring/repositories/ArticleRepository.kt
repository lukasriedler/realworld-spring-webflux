package com.github.lukasriedler.realworld.spring.repositories

import com.github.lukasriedler.realworld.spring.TimestampGenerator
import org.springframework.stereotype.Component
import java.text.Normalizer
import java.util.regex.Pattern

@Component
class ArticleRepository(private val timestampGenerator: TimestampGenerator) {

    private val nonLatin = Pattern.compile("[^\\w-]")
    private val whitespace = Pattern.compile("\\s")

    private val articles = HashMap<String, Article>()

    fun getArticles(
        filterTag: String?,
        filterAuthorUserId: UserId?,
        filterFavoritedUserId: UserId?,
        filterLimit: Int,
        filterOffset: Int
    ): List<Article> {
        val articles = articles.values.filter {
            (filterTag == null || it.tagList.contains(filterTag)) &&
                    (filterAuthorUserId == null || it.authorUserId == filterAuthorUserId) &&
                    (filterFavoritedUserId == null || it.favoritedBy.contains(filterFavoritedUserId))
        }
        if (filterOffset >= articles.size) {
            return listOf()
        }
        val lastIndexExclusive = (filterOffset + filterLimit).coerceAtMost(articles.size)
        return articles.subList(filterOffset, lastIndexExclusive)
    }

    fun getArticle(slug: String): Article? {
        return articles[slug]
    }

    fun createArticle(createArticle: CreateArticle, authorUserId: UserId): Article? {
        val slug = createSlug(createArticle.title)
        if (articles[slug] != null) {
            return null
        }
        val timestamp = timestampGenerator.createTimestamp()
        val article = Article(
            slug = slug,
            title = createArticle.title,
            description = createArticle.description,
            body = createArticle.body,
            tagList = createArticle.tagList?.toMutableSet() ?: mutableSetOf(),
            createdAt = timestamp,
            updatedAt = timestamp,
            authorUserId = authorUserId
        )
        articles[slug] = article
        return article
    }

    fun updateArticle(slug: String, updateArticle: UpdateArticle, email: String): Article? {
        val currentArticle = articles[slug] ?: return null
        var newArticle = currentArticle
        var newSlug = slug
        if (updateArticle.title != null) {
            newSlug = createSlug(updateArticle.title)
            if (articles[newSlug] != null) {
                return null
            }
            newArticle = newArticle.copy(slug = newSlug, title = updateArticle.title)
        }
        if (updateArticle.body != null) {
            newArticle = newArticle.copy(body = updateArticle.body)
        }
        if (updateArticle.description != null) {
            newArticle = newArticle.copy(description = updateArticle.description)
        }
        if (newSlug != slug) {
            articles.remove(slug)
        }
        val timestamp = timestampGenerator.createTimestamp()
        newArticle = newArticle.copy(updatedAt = timestamp)
        articles[newSlug] = newArticle
        return newArticle
    }

    fun deleteArticle(slug: String): Boolean {
        val removedArticle = articles.remove(slug)
        return removedArticle != null
    }

    fun getComments(slug: String): List<Comment>? {
        val article = articles[slug] ?: return null
        return article.comments.values.toList()
    }

    fun addComment(slug: String, addComment: AddComment, authorUserId: UserId): Comment? {
        val article = articles[slug] ?: return null
        val newCommentId = CommentId(article.latestCommentId + 1)

        val timestamp = timestampGenerator.createTimestamp()
        val comment = Comment(
            commentId = newCommentId,
            body = addComment.body,
            createdAt = timestamp,
            updatedAt = timestamp,
            authorUserId = authorUserId
        )
        article.comments[newCommentId] = comment
        article.latestCommentId = newCommentId.id
        return comment
    }

    fun deleteComment(slug: String, commentId: CommentId): Boolean {
        val article = articles[slug] ?: return false
        val comment = article.comments.remove(commentId)
        return comment != null
    }

    fun getTags(): List<String> {
        return articles.values.flatMap { it.tagList }.distinct().sorted()
    }

    private fun createSlug(title: String): String {
        val noWhitespace = whitespace.matcher(title).replaceAll("-")
        val normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD)
        return nonLatin.matcher(normalized).replaceAll("")
    }
}

data class CreateArticle(
    val title: String,
    val description: String,
    val body: String,
    val tagList: Set<String>?
)

data class UpdateArticle(
    val title: String?,
    val description: String?,
    val body: String?
)

data class AddComment(
    val body: String
)

data class CommentId(val id: Int)

data class Comment(
    val commentId: CommentId,
    val body: String,
    val createdAt: String,
    val updatedAt: String,
    val authorUserId: UserId
)

data class Article(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val tagList: MutableSet<String>,
    val createdAt: String,
    val updatedAt: String,
    val authorUserId: UserId,
    val favoritedBy: MutableSet<UserId> = mutableSetOf(),
    val comments: MutableMap<CommentId, Comment> = mutableMapOf(),
    var latestCommentId: Int = 0
)
