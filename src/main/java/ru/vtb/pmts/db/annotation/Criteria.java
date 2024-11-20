package ru.vtb.pmts.db.annotation;

import ru.vtb.pmts.db.entity.abstraction.ShardInstance;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Criteria {
    Class<? extends ShardInstance> from();
    String alias();
    Join[] joins() default {};
    String where() default "";
    CachePolicy cachePolicy() default @CachePolicy;
}
