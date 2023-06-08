package com.flipped.mybatis.audit.domain;

import com.flipped.mybatis.audit.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class City extends BaseEntity {

    private Integer id;

    private String name;

    private String shortName;
}
