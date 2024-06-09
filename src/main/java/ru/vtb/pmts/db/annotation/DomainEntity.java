package ru.vtb.pmts.db.annotation;

import org.apache.commons.lang3.StringUtils;
import ru.vtb.pmts.db.entity.abstraction.ShardInstance;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainEntity {
    Class<? extends ShardInstance> value();
    String cluster() default StringUtils.EMPTY;
    Storage storage() default @Storage;
    Storage[] additionalStorage() default {};
}
