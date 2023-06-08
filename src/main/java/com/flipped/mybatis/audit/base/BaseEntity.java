package com.flipped.mybatis.audit.base;

import com.flipped.mybatis.audit.annotation.UpdateBy;
import com.flipped.mybatis.audit.annotation.CreateBy;
import com.flipped.mybatis.audit.annotation.CreateTime;
import com.flipped.mybatis.audit.annotation.UpdateTime;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BaseEntity implements Serializable {

    /**
     * 创建者
     */
    @CreateBy
    private String createBy;

    /**
     * 创建时间
     */
    @CreateTime
    private Date createTime;

    @UpdateBy
    private String updateBy;

    /**
     * 更新时间
     */
    @UpdateTime
    private Date updateTime;
}
