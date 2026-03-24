package com.antalex.db.model.enums;

/**
 * Стратегия запроса
 */
public enum QueryStrategy {
    /**
     * Все шарды
     */
    ALL_SHARDS,
    /**
     * Собственная шарда
     */
    OWN_SHARD,
    /**
     * Шлавная шарда
     */
    MAIN_SHARD,
    /**
     * Новые шарды
     */
    NEW_SHARDS,
}
