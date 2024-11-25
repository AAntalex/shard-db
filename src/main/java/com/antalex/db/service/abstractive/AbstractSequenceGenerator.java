package com.antalex.db.service.abstractive;

import com.antalex.db.service.api.SequenceGenerator;
import lombok.Data;

import java.util.Objects;

@Data
public abstract class AbstractSequenceGenerator implements SequenceGenerator {
    protected Long value;
    protected Long minValue;
    protected Long maxValue;

    @Override
    public long curValue() {
        if (Objects.isNull(this.value)) {
            init();
        }
        return this.value;
    }

    @Override
    public synchronized long nextValue() {
        if (Objects.isNull(this.value)
                || Objects.nonNull(this.maxValue) && this.value > this.maxValue)
        {
            init();
        }
        return this.value++;
    }
}
