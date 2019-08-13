package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.request.AmountRequest
import com.ampnet.crowdfundingbackend.controller.pojo.response.DepositResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.DepositWithUserListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.DepositWithUserResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.persistence.model.Deposit
import com.ampnet.crowdfundingbackend.service.DepositService
import com.ampnet.crowdfundingbackend.service.pojo.ApproveDepositRequest
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import com.ampnet.crowdfundingbackend.service.pojo.MintServiceRequest
import com.ampnet.crowdfundingbackend.userservice.UserService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class DepositController(
    private val depositService: DepositService,
    private val userService: UserService
) {

    companion object : KLogging()

    @PostMapping("/api/v1/deposit")
    fun createDeposit(@RequestBody request: AmountRequest): ResponseEntity<DepositResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to create deposit" }
        val deposit = depositService.create(userPrincipal.uuid, request.amount)
        return ResponseEntity.ok(DepositResponse(deposit))
    }

    @GetMapping("/api/v1/deposit")
    fun getPendingDeposit(): ResponseEntity<DepositResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get pending deposit by user: ${userPrincipal.uuid}" }
        depositService.getPendingForUser(userPrincipal.uuid)?.let {
            return ResponseEntity.ok(DepositResponse(it))
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/api/v1/deposit/search")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getDepositByReference(
        @RequestParam("reference") reference: String
    ): ResponseEntity<DepositWithUserResponse> {
        logger.debug { "Received request to get find deposit by reference: $reference" }
        depositService.findByReference(reference)?.let {
            val user = userService.getUsers(listOf(it.userUuid)).firstOrNull()
            val response = DepositWithUserResponse(it, user)
            return ResponseEntity.ok(response)
        }
        return ResponseEntity.notFound().build()
    }

    @DeleteMapping("/api/v1/deposit/{id}")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_DEPOSIT)")
    fun deleteDeposit(@PathVariable("id") id: Int): ResponseEntity<Unit> {
        logger.debug { "Received request to delete deposit: $id" }
        depositService.delete(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/api/v1/deposit/{id}/approve")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_DEPOSIT)")
    fun approveDeposit(
        @PathVariable("id") id: Int,
        @RequestParam("amount") amount: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<DepositResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to approve deposit: $id" }

        val documentRequest = DocumentSaveRequest(file, userPrincipal.uuid)
        val serviceRequest = ApproveDepositRequest(id, userPrincipal.uuid, amount, documentRequest)
        val deposit = depositService.approve(serviceRequest)
        return ResponseEntity.ok(DepositResponse(deposit))
    }

    @GetMapping("/api/v1/deposit/unapproved")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getUnapprovedDeposits(): ResponseEntity<DepositWithUserListResponse> {
        logger.debug { "Received request to get unapproved deposits" }
        val deposits = depositService.getAllWithDocuments(false)
        val response = createDepositWithUserListResponse(deposits)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/api/v1/deposit/approved")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_DEPOSIT)")
    fun getApprovedDeposits(): ResponseEntity<DepositWithUserListResponse> {
        logger.debug { "Received request to get approved deposits" }
        val deposits = depositService.getAllWithDocuments(true)
        val response = createDepositWithUserListResponse(deposits)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/api/v1/deposit/{id}/transaction")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_DEPOSIT)")
    fun generateMintTransaction(@PathVariable("id") id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to generate mint transaction by user: ${userPrincipal.uuid}" }
        val serviceRequest = MintServiceRequest(id, userPrincipal.uuid)
        val transactionDataAndInfo = depositService.generateMintTransaction(serviceRequest)
        return ResponseEntity.ok(TransactionResponse(transactionDataAndInfo))
    }

    private fun createDepositWithUserListResponse(deposits: List<Deposit>): DepositWithUserListResponse {
        val users = userService.getUsers(deposits.map { it.userUuid })
        val depositWithUserList = mutableListOf<DepositWithUserResponse>()
        deposits.forEach { deposit ->
            val userUuid = deposit.userUuid.toString()
            val user = users.find { it.uuid == userUuid }
            depositWithUserList.add(DepositWithUserResponse(deposit, user))
        }
        return DepositWithUserListResponse(depositWithUserList)
    }
}
