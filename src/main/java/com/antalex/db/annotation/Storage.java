package com.antalex.db.annotation;

import com.antalex.db.model.enums.DataFormat;
import com.antalex.db.model.enums.ShardType;

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
