package com.antalex.db.service.api;

public interface SequenceGenerator {
    long nextValue();
    long curValue();
    void init();
}
