package com.rhkr8521.mapping.common.config.datasource;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Order(-1)  // 트랜잭션 어드바이스보다 먼저 실행되어야 함
@Component
public class DataSourceRoutingAspect {

    @Before("@annotation(transactional)")
    public void setDataSourceTypeBefore(Transactional transactional) {
        if (transactional.readOnly()) {
            // 읽기 전용 트랜잭션이면 SLAVE 사용
            DataSourceContextHolder.set(DataSourceType.SLAVE);
        } else {
            // 쓰기 트랜잭션 혹은 readOnly=false 면 MASTER 사용
            DataSourceContextHolder.set(DataSourceType.MASTER);
        }
    }

    @After("@annotation(transactional)")
    public void clearDataSourceTypeAfter(Transactional transactional) {
        DataSourceContextHolder.clear();
    }
}