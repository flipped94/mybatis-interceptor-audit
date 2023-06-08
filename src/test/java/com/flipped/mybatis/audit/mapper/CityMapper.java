package com.flipped.mybatis.audit.mapper;

import com.flipped.mybatis.audit.domain.City;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;


public interface CityMapper {

    @Insert("insert into city(name, short_name) values(#{name}, #{shortName})")
    Integer insert(City city);

    @Update("update city set name=#{name}, short_name=#{shortName} where id=#{id}")
    Integer update(City city);
}
