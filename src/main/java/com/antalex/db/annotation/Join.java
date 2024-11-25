package com.antalex.db.annotation;

import com.antalex.db.entity.abstraction.ShardInstance;

import javax.persistence.criteria.JoinType;
import java.lang.annotation.*;

@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface Join {
    Class<? extends ShardInstance> from();
    String alias();
    String on();
    JoinType joinType() default JoinType.INNER;
}
