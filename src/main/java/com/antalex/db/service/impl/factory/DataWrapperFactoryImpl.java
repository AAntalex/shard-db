package com.antalex.db.service.impl.factory;

import com.antalex.db.service.impl.wrapers.JSonWrapper;
import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.service.api.DataWrapper;
import com.antalex.db.service.api.DataWrapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class DataWrapperFactoryImpl implements DataWrapperFactory {
    private final ObjectMapper objectMapper;

    DataWrapperFactoryImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public DataWrapper createDataWrapper(DataFormat dataFormat) {
        if (dataFormat == DataFormat.JSON) {
            return new JSonWrapper(objectMapper);
        }
        return null;
    }
}
