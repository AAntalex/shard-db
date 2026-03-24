package com.antalex.db.model.enums;

/**
 * Тип шардирования
 */
public enum ShardType {
    /**
     * Реплицируемый
     */
    REPLICABLE,
    /**
     * Шардируемый
     */
    SHARDABLE,
    /**
     * Мульти-щардируемый
     */
    MULTI_SHARDABLE,
}
