package com.antalex.db.annotation;

import com.antalex.db.service.CriteriaCacheManager;
import com.antalex.db.service.impl.managers.TransactionalCacheManager;

import javax.persistence.FetchType;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface CachePolicy {
    Class<? extends CriteriaCacheManager> implement() default TransactionalCacheManager.class;
    FetchType fetch() default FetchType.LAZY;
    String[] key() default {};
    int retentionTime() default 0;
    int refreshTime() default 0;
}
