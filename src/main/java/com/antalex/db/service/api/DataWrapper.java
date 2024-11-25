package com.antalex.db.service.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DataWrapper {
    void init(String content) throws IOException;
    void put(String attribute, Object o) throws IOException;
    <T> T get(String attribute, Class<T> clazz) throws IOException;
    <K, V> Map<K, V> getMap(
            String attribute,
            Class<K> keyClazz,
            Class<V> valueClazz) throws IOException;
    <E> List<E> getList(String attribute, Class<E> clazz) throws IOException;
    String getContent();
}
