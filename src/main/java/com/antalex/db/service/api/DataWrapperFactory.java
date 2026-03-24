package com.antalex.db.service.api;

import com.antalex.db.model.enums.DataFormat;

public interface DataWrapperFactory {
    DataWrapper createDataWrapper(DataFormat dataFormat);
}
