package com.antalex.db.annotation;

import com.antalex.db.model.enums.MappingType;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Attribute {
    String name() default "";
    String storage() default "";
    MappingType mappingType() default MappingType.ENTITY;
}
