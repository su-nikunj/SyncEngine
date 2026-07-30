package org.example

data class Record (
    val id: Int,
    val data: Data,
    val lastModified: Long,
    val deleted: Boolean = false
)

data class Data (
    val intData: Int? = null,
    val stringData: String? = null,
    val floatData: Float? = null,
    val booleanData: Boolean? = null
)

enum class HttpMethod () { GET, POST, DELETE, PATCH }

data class HttpRequest (
    val method: HttpMethod,
    val record: Record
)

data class HttpResponse (
    val status: Int,
    val record: Record? = null
)