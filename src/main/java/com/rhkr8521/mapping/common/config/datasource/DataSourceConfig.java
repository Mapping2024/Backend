package com.rhkr8521.mapping.common.config.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final Environment env;

    // (1) Master DataSource
    @Bean(name = "masterDataSource")
    public DataSource masterDataSource(
            @Value("${spring.datasource.master.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.master.url}") String jdbcUrl,
            @Value("${spring.datasource.master.username}") String username,
            @Value("${spring.datasource.master.password}") String password,
            @Value("${spring.datasource.master.hikari.pool-name}") String poolName,
            @Value("${spring.datasource.master.hikari.maximum-pool-size}") int maxPoolSize
    ) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName(poolName);
        config.setMaximumPoolSize(maxPoolSize);
        return new HikariDataSource(config);
    }

    // (2) Slave DataSource
    @Bean(name = "slaveDataSource")
    public DataSource slaveDataSource(
            @Value("${spring.datasource.slave.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.slave.url}") String jdbcUrl,
            @Value("${spring.datasource.slave.username}") String username,
            @Value("${spring.datasource.slave.password}") String password,
            @Value("${spring.datasource.slave.hikari.pool-name}") String poolName,
            @Value("${spring.datasource.slave.hikari.maximum-pool-size}") int maxPoolSize
    ) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName(poolName);
        config.setMaximumPoolSize(maxPoolSize);
        return new HikariDataSource(config);
    }

    // ReplicationRoutingDataSource 설정
    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("slaveDataSource") DataSource slaveDataSource
    ) {
        ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER, masterDataSource);
        targetDataSources.put(DataSourceType.SLAVE, slaveDataSource);

        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        return routingDataSource;
    }

    // LazyConnectionDataSourceProxy로 감싸기
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource(
            @Qualifier("routingDataSource") DataSource routingDataSource
    ) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

    // EntityManagerFactory 설정 (네이밍 전략을 실제 객체로 지정)
    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("dataSource") DataSource dataSource
    ) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.rhkr8521.mapping.api"); // 엔티티 패키지 경로

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        // Hibernate 속성들
        Properties props = new Properties();
        props.put("hibernate.dialect", env.getProperty("spring.jpa.hibernate.dialect"));
        props.put("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        props.put("hibernate.show_sql", env.getProperty("spring.jpa.show-sql"));
        props.put("hibernate.format_sql", env.getProperty("spring.jpa.properties.hibernate.format_sql"));

        // 실제 네이밍 전략 객체를 빈으로 등록 -> CamelCase → snake_case 매핑이 정상 동작
        //  1) ImplicitNamingStrategy
        props.put("hibernate.implicit_naming_strategy",
                SpringImplicitNamingStrategy.class.getName());
        //  2) PhysicalNamingStrategy
        props.put("hibernate.physical_naming_strategy",
                CamelCaseToUnderscoresNamingStrategy.class.getName());

        emf.setJpaProperties(props);
        return emf;
    }

    // TransactionManager 설정
    @Primary
    @Bean
    public JpaTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean emf
    ) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(emf.getObject());
        return txManager;
    }
}