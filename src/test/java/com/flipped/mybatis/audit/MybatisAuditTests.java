package com.flipped.mybatis.audit;

import com.flipped.mybatis.audit.context.AuditFiledContext;
import com.flipped.mybatis.audit.domain.City;
import com.flipped.mybatis.audit.mapper.CityMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;


public class MybatisAuditTests {

    private CityMapper cityMapper;

    @BeforeClass
    public void prepare() throws IOException {
        AuditFiledContext.setCurrentUser("flipped");
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession sqlSession = sqlSessionFactory.openSession(true);
        cityMapper = sqlSession.getMapper(CityMapper.class);
    }


    @Test
    public void insert() {
        City city = new City();
        city.setName("重庆");
        city.setShortName("CQ");
        cityMapper.insert(city);
    }


    @Test
    public void update() {
        City city = new City();
        city.setId(6);
        city.setName("重庆渝中");
        city.setShortName("CQYZ");
        cityMapper.update(city);
    }

}
