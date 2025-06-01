package com.rhkr8521.mapping.common.config.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        DataSourceType dataSourceType = DataSourceContextHolder.get();
        // 기본값을 MASTER 로 (쓰기, 혹은 설정이 없을 때)
        return (dataSourceType == null) ? DataSourceType.MASTER : dataSourceType;
    }
}