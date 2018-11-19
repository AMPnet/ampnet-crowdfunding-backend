package com.ampnet.crowdfundingbackend.persistence.model

import java.io.Serializable

class OrganizationUserCompositeId() : Serializable {

    protected var organizationId: Int = -1
    protected var userId: Int = -1

    constructor(organizationId: Int, userId: Int): this() {
        this.organizationId = organizationId
        this.userId = userId
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrganizationUserCompositeId

        if (organizationId != other.organizationId) return false
        if (userId != other.userId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = organizationId
        result = 31 * result + userId
        return result
    }
}
