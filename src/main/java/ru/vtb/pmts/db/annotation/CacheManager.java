package ru.vtb.pmts.db.annotation;

import ru.vtb.pmts.db.service.CriteriaCacheManager;
import ru.vtb.pmts.db.service.impl.managers.TransactionalCacheManager;

import javax.persistence.FetchType;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheManager {
    Class<? extends CriteriaCacheManager> implement() default TransactionalCacheManager.class;
    FetchType fetch() default FetchType.LAZY;
    String[] key() default {};
    int refreshTimeOut() default 0;
}
