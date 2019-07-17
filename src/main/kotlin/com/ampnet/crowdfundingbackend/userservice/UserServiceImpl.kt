package com.ampnet.crowdfundingbackend.userservice

import com.ampnet.crowdfundingbackend.userservice.pojo.UserResponse
import com.ampnet.userservice.proto.GetUsersRequest
import com.ampnet.userservice.proto.UserServiceGrpc
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory
) : UserService {

    companion object : KLogging()

    private val serviceBlockingStub: UserServiceGrpc.UserServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("user-service")
        UserServiceGrpc.newBlockingStub(channel)
    }

    override fun getUsers(uuids: List<UUID>): List<UserResponse> {
        logger.debug { "Fetching users: $uuids" }
        val request = GetUsersRequest.newBuilder()
                .addAllUuids(uuids.map { it.toString() })
                .build()
        return serviceBlockingStub.getUsers(request).usersList.map { UserResponse(it) }
    }
}
