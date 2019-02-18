package com.ampnet.crowdfundingbackend.persistence

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import org.apache.lucene.search.Query
import org.hibernate.search.jpa.FullTextQuery
import org.hibernate.search.jpa.Search
import org.hibernate.search.query.dsl.QueryBuilder
import org.springframework.stereotype.Repository
import javax.persistence.EntityManager

@Repository
class SearchDao(private val entityManager: EntityManager) {

    fun searchOrganizationByName(name: String): List<Organization> {
        val clazz = Organization::class.java
        val query = getQueryBuilder(clazz)
            .keyword()
            .fuzzy()
            .withEditDistanceUpTo(2)
            .onField("name")
            .matching(name)
            .createQuery()

        return getJpaQuery(query, clazz).resultList as List<Organization>
    }

    private fun getJpaQuery(query: Query, clazz: Class<*>): FullTextQuery {
        val fullTextEntityManager = getFullTextEm()
        return fullTextEntityManager.createFullTextQuery(query, clazz)
    }

    private fun getQueryBuilder(clazz: Class<*>): QueryBuilder {
        val fullTextEntityManager = getFullTextEm()
        return fullTextEntityManager.searchFactory.buildQueryBuilder().forEntity(clazz).get()
    }

    private fun getFullTextEm() = Search.getFullTextEntityManager(entityManager)
}
