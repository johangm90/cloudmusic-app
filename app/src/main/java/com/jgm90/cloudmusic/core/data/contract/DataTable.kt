package com.jgm90.cloudmusic.core.data.contract

interface DataTable<T> {
    fun getOne(filter: String): T?

    fun getAll(): List<T>

    fun insert(obj: T)

    fun update(obj: T)

    fun delete(obj: T)
}
