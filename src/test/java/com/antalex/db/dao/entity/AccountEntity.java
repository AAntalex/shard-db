package com.antalex.db.dao.entity;

import com.antalex.db.annotation.ParentShard;
import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Table(
       indexes = {
               @Index(columnList = "code"),
               @Index(columnList = "dateOpen")
        })
@Data
@Accessors(chain = true, fluent = true)
@ShardEntity(type = ShardType.SHARDABLE)
public class AccountEntity extends BaseShardEntity {
    private String code;
    @ParentShard
    @OneToOne
    @JoinColumn
    private ClientEntity client;
    private BigDecimal balance;
    private OffsetDateTime dateOpen;
}
