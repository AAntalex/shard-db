package com.antalex.db.dao.entity;

import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.Table;

@EqualsAndHashCode(callSuper = true)
@Table(name = "T_CLIENT_CATEGORY")
@Data
@Accessors(chain = true, fluent = true)
@ShardEntity(type = ShardType.REPLICABLE)
public class ClientCategoryEntity extends BaseShardEntity {
    private String code;
}
