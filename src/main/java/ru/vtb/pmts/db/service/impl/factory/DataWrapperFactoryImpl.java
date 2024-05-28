package ru.vtb.pmts.db.service.impl.factory;

import ru.vtb.pmts.db.model.enums.DataFormat;
import ru.vtb.pmts.db.service.api.DataWrapper;
import ru.vtb.pmts.db.service.api.DataWrapperFactory;
import ru.vtb.pmts.db.service.impl.wrapers.JSonWrapper;
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
