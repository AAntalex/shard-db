package com.antalex.db.annotation;

import com.antalex.db.entity.abstraction.ShardInstance;

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
