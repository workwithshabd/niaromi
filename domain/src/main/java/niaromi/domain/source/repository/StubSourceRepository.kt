package niaromi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import niaromi.domain.source.model.StubSource

interface StubSourceRepository {
    fun subscribeAll(): Flow<List<StubSource>>

    suspend fun getStubSource(id: Long): StubSource?

    suspend fun upsertStubSource(id: Long, lang: String, name: String)
}
