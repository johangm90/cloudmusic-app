package com.jgm90.cloudmusic.interfaces;

import java.util.List;

public interface DataTable<T> {

    T getOne(String filter);

    List<T> getAll();

    void insert(T obj);

    void update(T obj);

    void delete(T obj);
}
