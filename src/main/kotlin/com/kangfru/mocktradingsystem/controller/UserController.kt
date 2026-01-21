package com.kangfru.mocktradingsystem.controller

import com.kangfru.mocktradingsystem.domain.User
import com.kangfru.mocktradingsystem.service.AccountService
import com.kangfru.mocktradingsystem.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val accountService: AccountService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createUser(
        @RequestBody request: CreateUserRequest,
    ): UserResponse {
        val user = userService.createUser(request.username, request.email)
        return user.toResponse()
    }

    @GetMapping("/{seq}")
    suspend fun getUser(
        @PathVariable seq: Long,
    ): UserResponse {
        val user = userService.getUser(seq)
        return user.toResponse()
    }

    @GetMapping
    suspend fun getAllUsers(): List<UserResponse> = userService.getAllUsers().map { it.toResponse() }

    @PutMapping("/{seq}")
    suspend fun updateUser(
        @PathVariable seq: Long,
        @RequestBody request: UpdateUserRequest,
    ): UserResponse {
        val user = userService.updateUser(seq, request.username, request.email)
        return user.toResponse()
    }

    @DeleteMapping("/{seq}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteUser(
        @PathVariable seq: Long,
    ) {
        userService.deleteUser(seq)
    }

    @GetMapping("/{seq}/accounts")
    suspend fun getUserAccounts(
        @PathVariable seq: Long,
    ): List<AccountSummaryResponse> {
        val accounts = accountService.getAccountsByUserId(seq)
        return accounts.map { account ->
            AccountSummaryResponse(
                accountNumber = account.accountNumber,
                balance = account.balance,
                totalAssets = account.totalAssets,
                holdingsCount = account.holdings.size,
            )
        }
    }

    @GetMapping("/email/{email}")
    suspend fun getUserByEmail(
        @PathVariable email: String,
    ): UserResponse {
        val user = userService.getUserByEmail(email)
        return user.toResponse()
    }

    private fun User.toResponse() =
        UserResponse(
            seq = seq,
            username = username,
            email = email,
            accountNumbers = accountNumbers,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}

data class CreateUserRequest(
    val username: String,
    val email: String,
)

data class UpdateUserRequest(
    val username: String? = null,
    val email: String? = null,
)

data class UserResponse(
    val seq: Long,
    val username: String,
    val email: String,
    val accountNumbers: List<String>,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime,
)

data class AccountSummaryResponse(
    val accountNumber: String,
    val balance: java.math.BigDecimal,
    val totalAssets: java.math.BigDecimal,
    val holdingsCount: Int,
)
