package ru.vtb.pmts.db.service.api;

import ru.vtb.pmts.db.model.enums.DataFormat;

public interface DataWrapperFactory {
    DataWrapper createDataWrapper(DataFormat dataFormat);
}
