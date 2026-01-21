package com.kangfru.mocktradingsystem.controller

import com.kangfru.mocktradingsystem.domain.Account
import com.kangfru.mocktradingsystem.domain.StockHolding
import com.kangfru.mocktradingsystem.service.AccountService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/accounts")
class AccountController(
    private val accountService: AccountService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createAccount(@RequestBody request: CreateAccountRequest): AccountResponse {
        val account = accountService.createAccount(request.userId, request.initialBalance ?: BigDecimal.ZERO)
        return account.toResponse()
    }

    @GetMapping("/{accountNumber}")
    suspend fun getAccount(@PathVariable accountNumber: String): AccountResponse {
        val account = accountService.getAccount(accountNumber)
        return account.toResponse()
    }

    @GetMapping
    suspend fun getAllAccounts(): List<AccountResponse> {
        return accountService.getAllAccounts().map { it.toResponse() }
    }

    @DeleteMapping("/{accountNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun deleteAccount(@PathVariable accountNumber: String) {
        accountService.deleteAccount(accountNumber)
    }

    @PostMapping("/{accountNumber}/deposit")
    suspend fun deposit(
        @PathVariable accountNumber: String,
        @RequestBody request: DepositRequest,
    ): AccountResponse {
        val account = accountService.deposit(accountNumber, request.amount)
        return account.toResponse()
    }

    @PostMapping("/{accountNumber}/withdraw")
    suspend fun withdraw(
        @PathVariable accountNumber: String,
        @RequestBody request: WithdrawRequest,
    ): AccountResponse {
        val account = accountService.withdraw(accountNumber, request.amount)
        return account.toResponse()
    }

    @PostMapping("/{accountNumber}/stocks/buy")
    suspend fun buyStock(
        @PathVariable accountNumber: String,
        @RequestBody request: BuyStockRequest,
    ): AccountResponse {
        val account = accountService.buyStock(
            accountNumber,
            request.stockCode,
            request.quantity,
            request.price,
        )
        return account.toResponse()
    }

    @PostMapping("/{accountNumber}/stocks/sell")
    suspend fun sellStock(
        @PathVariable accountNumber: String,
        @RequestBody request: SellStockRequest,
    ): AccountResponse {
        val account = accountService.sellStock(
            accountNumber,
            request.stockCode,
            request.quantity,
            request.price,
        )
        return account.toResponse()
    }

    @GetMapping("/{accountNumber}/holdings")
    suspend fun getHoldings(@PathVariable accountNumber: String): Map<String, HoldingResponse> {
        val holdings = accountService.getHoldings(accountNumber)
        return holdings.mapValues { (_, holding) ->
            HoldingResponse(
                stockCode = holding.stockCode,
                quantity = holding.quantity,
                averagePrice = holding.averagePrice,
                totalValue = holding.totalValue,
            )
        }
    }

    @GetMapping("/{accountNumber}/holdings/{stockCode}")
    suspend fun getHolding(
        @PathVariable accountNumber: String,
        @PathVariable stockCode: String,
    ): HoldingResponse? {
        val holding = accountService.getHolding(accountNumber, stockCode)
        return holding?.let {
            HoldingResponse(
                stockCode = it.stockCode,
                quantity = it.quantity,
                averagePrice = it.averagePrice,
                totalValue = it.totalValue,
            )
        }
    }

    private fun Account.toResponse() = AccountResponse(
        accountNumber = accountNumber,
        userId = userId,
        balance = balance,
        holdings = holdings.mapValues { (_, holding) ->
            HoldingResponse(
                stockCode = holding.stockCode,
                quantity = holding.quantity,
                averagePrice = holding.averagePrice,
                totalValue = holding.totalValue,
            )
        },
        totalAssets = totalAssets,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

data class CreateAccountRequest(
    val userId: Long,
    val initialBalance: BigDecimal? = null
)

data class DepositRequest(
    val amount: BigDecimal
)

data class WithdrawRequest(
    val amount: BigDecimal
)

data class BuyStockRequest(
    val stockCode: String,
    val quantity: Int,
    val price: BigDecimal
)

data class SellStockRequest(
    val stockCode: String,
    val quantity: Int,
    val price: BigDecimal
)

data class AccountResponse(
    val accountNumber: String,
    val userId: Long,
    val balance: BigDecimal,
    val holdings: Map<String, HoldingResponse>,
    val totalAssets: BigDecimal,
    val createdAt: java.time.LocalDateTime,
    val updatedAt: java.time.LocalDateTime
)

data class HoldingResponse(
    val stockCode: String,
    val quantity: Int,
    val averagePrice: BigDecimal,
    val totalValue: BigDecimal
)
