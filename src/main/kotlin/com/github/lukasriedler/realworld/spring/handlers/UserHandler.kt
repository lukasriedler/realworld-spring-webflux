package com.github.lukasriedler.realworld.spring.handlers

import com.github.lukasriedler.realworld.spring.JwtGenerator
import com.github.lukasriedler.realworld.spring.dtos.*
import com.github.lukasriedler.realworld.spring.endpoints.createErrorDto
import com.github.lukasriedler.realworld.spring.endpoints.createNotFoundDto
import com.github.lukasriedler.realworld.spring.endpoints.getEmail
import com.github.lukasriedler.realworld.spring.repositories.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.server.ResponseStatusException

@Component
class UserHandler(private val repository: UserRepository, private val jwtGenerator: JwtGenerator) {

    suspend fun createUser(request: ServerRequest): ServerResponse {
        val createUserDto = request.awaitBody<CreateUserWrapperDto>().user
        val createUser = CreateUser(
            username = createUserDto.username,
            email = createUserDto.email,
            password = createUserDto.password
        )
        val user = repository.createUser(createUser) ?: return createErrorDto(listOf("user already exists"))
        return ServerResponse.ok().bodyValueAndAwait(user.toUserDto())
    }

    suspend fun loginUser(request: ServerRequest): ServerResponse {
        val loginUserDto = request.awaitBody<LoginUserWrapperDto>().user
        val loginUser = LoginUser(
            email = loginUserDto.email,
            password = loginUserDto.password
        )
        val user = repository.loginUser(loginUser) ?: return createErrorDto(listOf("login failed"))
        return ServerResponse.ok().bodyValueAndAwait(user.toUserDto())
    }

    suspend fun getUser(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val user = repository.getUserByEmail(email) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        return ServerResponse.ok().bodyValueAndAwait(user.toUserDto())
    }

    suspend fun updateUser(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val updateUserDto = request.awaitBody<UpdateUserWrapperDto>().user
        val updateUser = UpdateUser(
            username = updateUserDto.username,
            email = updateUserDto.email,
            password = updateUserDto.password,
            bio = updateUserDto.bio,
            image = updateUserDto.image
        )
        val user =
            repository.updateUser(email, updateUser) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        return ServerResponse.ok().bodyValueAndAwait(user.toUserDto())
    }

    suspend fun getProfile(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val currentUser = repository.getUserByEmail(email)
        val username = request.pathVariable("username")
        val user = repository.getUserByUsername(username) ?: return createNotFoundDto()
        val following = currentUser?.following?.contains(user.userId) ?: false
        return ServerResponse.ok().bodyValueAndAwait(user.toProfileDto(following))
    }

    suspend fun followUser(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val usernameToFollow = request.pathVariable("username")
        val userToFollow = repository.getUserByUsername(usernameToFollow) ?: return createNotFoundDto()
        val followed = repository.followUser(email, userToFollow)
        if (!followed) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        val profile = userToFollow.toProfileDto(following = true)
        return ServerResponse.ok().bodyValueAndAwait(profile)
    }

    suspend fun unfollowUser(request: ServerRequest): ServerResponse {
        val email = request.getEmail(jwtGenerator)
        val usernameToUnfollow = request.pathVariable("username")
        val userToFollow = repository.getUserByUsername(usernameToUnfollow) ?: return createNotFoundDto()
        val unfollowed = repository.unfollowUser(email, userToFollow)
        if (!unfollowed) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        val profile = userToFollow.toProfileDto(following = false)
        return ServerResponse.ok().bodyValueAndAwait(profile)
    }

    private fun User.toUserDto(): UserWrapperDto {
        val token =
            jwtGenerator.generateToken(this.email) ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)
        return UserWrapperDto(
            user = UserDto(
                email = this.email,
                token = token,
                username = this.username,
                bio = this.bio,
                image = this.image
            )
        )
    }

    private fun User.toProfileDto(following: Boolean): ProfileWrapperDto {
        return ProfileWrapperDto(
            profile = ProfileDto(
                username = this.username,
                bio = this.bio,
                image = this.image,
                following = following
            )
        )
    }
}
