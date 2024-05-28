package ru.vtb.pmts.db.annotation;

import ru.vtb.pmts.db.model.enums.MappingType;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Attribute {
    String name() default "";
    String storage() default "";
    MappingType mappingType() default MappingType.ENTITY;
}
