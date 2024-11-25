package com.antalex.db.annotation;

import com.antalex.db.entity.abstraction.ShardInstance;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainEntity {
    Class<? extends ShardInstance> value();
    String cluster() default "";
    Storage storage() default @Storage;
    Storage[] additionalStorage() default {};
}
