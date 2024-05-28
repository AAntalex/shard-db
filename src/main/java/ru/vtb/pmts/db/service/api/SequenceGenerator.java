package ru.vtb.pmts.db.service.api;

public interface SequenceGenerator {
    long nextValue();
    long curValue();
    void init();
}
