package com.github.lukasriedler.realworld.spring.dtos

data class CreateUserWrapperDto(
    val user: CreateUserDto
)

data class CreateUserDto(
    val email: String,
    val username: String,
    val password: String
)

data class LoginUserWrapperDto(
    val user: LoginUserDto
)

data class LoginUserDto(
    val email: String,
    val password: String
)

data class UpdateUserWrapperDto(
    val user: UpdateUserDto
)

data class UpdateUserDto(
    val email: String?,
    val username: String?,
    val password: String?,
    val bio: String?,
    val image: String?
)

data class UserWrapperDto(
    val user: UserDto
)

data class UserDto(
    val email: String,
    val token: String,
    val username: String,
    val bio: String?,
    val image: String?
)

data class ProfileWrapperDto(
    val profile: ProfileDto
)

data class ProfileDto(
    val username: String,
    val bio: String?,
    val image: String?,
    val following: Boolean
)

data class CreateArticleWrapperDto(
    val article: CreateArticleDto
)

data class CreateArticleDto(
    val title: String,
    val description: String,
    val body: String,
    val tagList: Set<String>?
)

data class UpdateArticleWrapperDto(
    val article: UpdateArticleDto
)

data class UpdateArticleDto(
    val title: String?,
    val description: String?,
    val body: String?
)

data class ArticleWrapperDto(
    val article: ArticleDto
)

data class ArticleDto(
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val tagList: List<String>,
    val createdAt: String,
    val updatedAt: String,
    val favorited: Boolean,
    val favoritesCount: Int,
    val author: ProfileDto
)

data class ArticlesDto(val articles: List<ArticleDto>, val articlesCount: Int)

data class CreateCommentDto(
    val body: String
)

data class CreateCommentWrapperDto(
    val comment: CreateCommentDto
)

data class CommentWrapperDto(
    val comment: CommentDto
)

data class CommentsDto(
    val comments: List<CommentDto>
)

data class CommentDto(
    val id: Int,
    val createdAt: String,
    val updatedAt: String,
    val body: String,
    val author: ProfileDto
)

data class TagsDto(
    val tags: List<String>
)

data class ErrorsWrapperDto(
    val errors: ErrorsDto
)

data class ErrorsDto(
    val body: List<String>
)
