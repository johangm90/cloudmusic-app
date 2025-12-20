package com.jgm90.cloudmusic.interfaces

interface DataTable<T> {
    fun getOne(filter: String): T?

    fun getAll(): List<T>

    fun insert(obj: T)

    fun update(obj: T)

    fun delete(obj: T)
}
