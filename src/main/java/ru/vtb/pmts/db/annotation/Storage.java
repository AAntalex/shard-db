package ru.vtb.pmts.db.annotation;

import ru.vtb.pmts.db.model.enums.DataFormat;
import ru.vtb.pmts.db.model.enums.ShardType;

import javax.persistence.FetchType;
import java.lang.annotation.*;

@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Storage {
    String value() default "DEFAULT";
    String cluster() default "";
    ShardType shardType() default ShardType.SHARDABLE;
    DataFormat dataFormat() default DataFormat.JSON;
    FetchType fetchType() default FetchType.EAGER;
}
