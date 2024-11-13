package ru.vtb.pmts.db.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CriteriaAttribute {
    String value();
}
