package com.antalex.db.annotation;

import com.antalex.db.entity.abstraction.ShardInstance;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainEntity {
    Class<? extends ShardInstance> value();
    String cluster() default StringUtils.EMPTY;
    Storage storage() default @Storage("<DEFAULT>");
    Storage[] additionalStorage() default {};
}
