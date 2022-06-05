package com.github.lukasriedler.realworld.spring.repositories

import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random

@Component
class UserRepository {

    private val users = HashMap<UserId, User>()

    fun createUser(createUser: CreateUser): User? {
        if (ensureUsernameAndEmailUnique(createUser.username, createUser.email)) {
            return null
        }
        val salt = createSalt()
        val newUser = User(
            userId = createNewUniqueUuid(),
            username = createUser.username,
            email = createUser.email,
            salt = salt,
            hashedPassword = saltAndHashPassword(createUser.password, salt),
            bio = null,
            image = null,
            following = HashSet()
        )
        users[newUser.userId] = newUser
        return newUser
    }

    fun ensureUsernameAndEmailUnique(username: String, email: String): Boolean {
        return users.values.any { it.username == username || it.email == email }
    }

    fun loginUser(loginUser: LoginUser): User? {
        val user = getUserByEmail(loginUser.email) ?: return null
        val hashedPassword = saltAndHashPassword(loginUser.password, user.salt)
        if (user.hashedPassword == hashedPassword) {
            return user
        }
        return null
    }

    fun getUserByEmail(email: String): User? {
        return users.values.firstOrNull { it.email == email }
    }

    fun getUserByUsername(username: String): User? {
        return users.values.firstOrNull { it.username == username }
    }

    fun getUser(userId: UserId): User? {
        return users[userId]
    }

    fun updateUser(email: String, updateUser: UpdateUser): User? {
        var user = getUserByEmail(email) ?: return null
        if (updateUser.email != null) {
            user = user.copy(email = updateUser.email)
        }
        if (updateUser.username != null) {
            user = user.copy(username = updateUser.username)
        }
        if (updateUser.password != null) {
            val salt = createSalt()
            val hashedPassword = saltAndHashPassword(updateUser.password, salt)
            user = user.copy(salt = salt, hashedPassword = hashedPassword)
        }
        if (updateUser.bio != null) {
            user = user.copy(bio = updateUser.bio)
        }
        if (updateUser.image != null) {
            user = user.copy(image = updateUser.image)
        }
        return user
    }

    fun followUser(email: String, userToFollow: User): Boolean {
        val user = getUserByEmail(email) ?: return false
        user.following.add(userToFollow.userId)
        return true
    }

    fun unfollowUser(email: String, userToUnfollow: User): Boolean {
        val user = getUserByEmail(email) ?: return false
        user.following.remove(userToUnfollow.userId)
        return true
    }

    private fun createSalt(): Long = Random.Default.nextLong()

    private fun saltAndHashPassword(password: String, salt: Long): String {
        val sha512 = MessageDigest.getInstance("SHA-512")
        sha512.update(salt.toString().toByteArray())
        sha512.update(password.toByteArray())
        return sha512.digest().decodeToString()
    }

    private fun createNewUniqueUuid(): UserId {
        var newUserId: UserId
        do {
            newUserId = UserId(UUID.randomUUID())
        } while (users.containsKey(newUserId))
        return newUserId;
    }
}

data class UserId(val id: UUID)

data class LoginUser(
    val email: String,
    val password: String
)

data class CreateUser(
    val username: String,
    val email: String,
    val password: String
)

data class UpdateUser(
    val email: String?,
    val username: String?,
    val password: String?,
    val bio: String?,
    val image: String?
)

data class User(
    val userId: UserId,
    val username: String,
    val email: String,
    val salt: Long,
    val hashedPassword: String,
    val bio: String?,
    val image: String?,
    val following: MutableSet<UserId>
)
