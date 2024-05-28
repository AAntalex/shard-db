package ru.vtb.pmts.db.model.enums;

/**
 * Статус задачи
 */
public enum TaskStatus {
    /**
     * Создано
     */
    CREATED,
    /**
     * Запущено
     */
    RUNNING,
    /**
     * Выполнено
     */
    DONE,
    /**
     * Завершение
     */
    COMPLETION,
    /**
     * Завершено
     */
    FINISHED,
}
