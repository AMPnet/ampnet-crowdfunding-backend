package com.ampnet.crowdfundingbackend.userservice

import com.ampnet.userservice.proto.GetUsersRequest
import com.ampnet.userservice.proto.UserResponse
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
        val set = uuids.toSet()
        logger.debug { "Fetching users: $set" }
        val request = GetUsersRequest.newBuilder()
                .addAllUuids(set.map { it.toString() })
                .build()
        return serviceBlockingStub.getUsers(request).usersList
    }
}
