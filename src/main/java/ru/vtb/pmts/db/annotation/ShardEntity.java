package ru.vtb.pmts.db.annotation;

import ru.vtb.pmts.db.model.enums.ShardType;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ShardEntity {
    ShardType type() default ShardType.SHARDABLE;
    String cluster() default "DEFAULT";
    String tablePrefix() default "T_";
    String columnPrefix() default "C_";
}
