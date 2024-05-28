package ru.vtb.pmts.db.service.impl;

import ru.vtb.pmts.db.service.abstractive.AbstractSequenceGenerator;

public class SimpleSequenceGenerator extends AbstractSequenceGenerator {
    public SimpleSequenceGenerator(Long minValue, Long maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    SimpleSequenceGenerator(Long minValue) {
        this.minValue = minValue;
    }

    SimpleSequenceGenerator() {
        this.minValue = 0L;
    }

    @Override
    public void init() {
        this.value = this.minValue;
    }
}
