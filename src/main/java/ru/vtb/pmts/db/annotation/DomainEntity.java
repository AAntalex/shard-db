package ru.vtb.pmts.db.annotation;

import ru.vtb.pmts.db.entity.abstraction.ShardInstance;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainEntity {
    Class<? extends ShardInstance> value();
    Storage storage() default @Storage("<DEFAULT>");
    Storage[] additionalStorage() default {};
}
