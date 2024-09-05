package com.antalex.db.model.enums;

/**
 * Тип запроса
 */
public enum QueryType {
    /**
     * Выборка
     */
    SELECT,
    /**
     * DML (INSERT, UPDATE, DELETE)
     */
    DML,
    /**
     * Блокировка
     */
    LOCK,
}
