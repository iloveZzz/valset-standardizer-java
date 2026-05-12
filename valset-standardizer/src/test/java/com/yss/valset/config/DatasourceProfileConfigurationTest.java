package com.yss.valset.config;

import com.yss.valset.extract.config.YssDataMybatisConfig;
import com.yss.valset.common.support.DatabaseDialectSupport;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatasourceProfileConfigurationTest {

    private final YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();

    @Test
    void applicationYamlDeclaresMysqlAsDefaultProfileAndIncludesNacos() throws IOException {
        String content = readClasspathText("application.yml");

        assertThat(content).contains("group:");
        assertThat(content).contains("mysql:");
        assertThat(content).contains("postgresql:");
        assertThat(content).contains("active: ${SPRING_PROFILES_ACTIVE:mysql}");
    }

    @Test
    void mysqlProfileDeclaresMysqlDatasource() throws IOException {
        PropertySource<?> propertySource = loadYaml("application-mysql.yml");

        assertThat(propertySource.getProperty("spring.datasource.primary.url"))
                .isEqualTo("jdbc:mysql://127.0.0.1:3308/t_datamiddle_subject?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai");
        assertThat(propertySource.getProperty("spring.datasource.primary.username"))
                .isEqualTo("${VALSET_DATASOURCE_USERNAME:root}");
        assertThat(propertySource.getProperty("spring.datasource.primary.driver-class-name"))
                .isEqualTo("com.mysql.cj.jdbc.Driver");
    }

    @Test
    void postgresqlProfileDeclaresPostgresqlDatasource() throws IOException {
        PropertySource<?> propertySource = loadYaml("application-postgresql.yml");

        assertThat(propertySource.getProperty("spring.datasource.primary.url"))
                .isEqualTo("jdbc:postgresql://127.0.0.1:5432/t_datamiddle_subject?currentSchema=public");
        assertThat(propertySource.getProperty("spring.datasource.primary.username"))
                .isEqualTo("${VALSET_DATASOURCE_USERNAME:root}");
        assertThat(propertySource.getProperty("spring.datasource.primary.driver-class-name"))
                .isEqualTo("org.postgresql.Driver");
    }

    @Test
    void oracleProfileDeclaresOracleDatasource() throws IOException {
        PropertySource<?> propertySource = loadYaml("application-oracle.yml");

        assertThat(propertySource.getProperty("spring.datasource.primary.url"))
                .isEqualTo("jdbc:oracle:thin:@//127.0.0.1:1521/orclpdb1");
        assertThat(propertySource.getProperty("spring.datasource.primary.username"))
                .isEqualTo("${VALSET_DATASOURCE_USERNAME:valset}");
        assertThat(propertySource.getProperty("spring.datasource.primary.driver-class-name"))
                .isEqualTo("oracle.jdbc.OracleDriver");
    }

    @Test
    void databaseIdProviderNormalizesVendorNames() throws Exception {
        DatabaseIdProvider databaseIdProvider = new YssDataMybatisConfig().databaseIdProvider();

        assertThat(databaseIdProvider.getDatabaseId(mockDataSource("MySQL"))).isEqualTo("mysql");
        assertThat(databaseIdProvider.getDatabaseId(mockDataSource("PostgreSQL"))).isEqualTo("postgresql");
        assertThat(databaseIdProvider.getDatabaseId(mockDataSource("Oracle"))).isEqualTo("oracle");
    }

    @Test
    void databaseDialectSupportUsesOracleFetchClause() {
        DatabaseDialectSupport databaseDialectSupport = new DatabaseDialectSupport(mockDataSource("Oracle"));

        assertThat(databaseDialectSupport.isOracle()).isTrue();
        assertThat(databaseDialectSupport.limitClause(10)).isEqualTo("fetch first 10 rows only");
    }

    private PropertySource<?> loadYaml(String resourcePath) throws IOException {
        List<PropertySource<?>> sources = yamlPropertySourceLoader.load(resourcePath, new ClassPathResource(resourcePath));
        assertThat(sources).isNotEmpty();
        return sources.get(0);
    }

    private String readClasspathText(String resourcePath) throws IOException {
        try (InputStream inputStream = new ClassPathResource(resourcePath).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private DataSource mockDataSource(String databaseProductName) {
        DatabaseMetaData databaseMetaData = (DatabaseMetaData) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{DatabaseMetaData.class},
                new DatabaseMetaDataInvocationHandler(databaseProductName));
        Connection connection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("getMetaData".equals(method.getName())) {
                        return databaseMetaData;
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return Boolean.FALSE;
                    }
                    return defaultValue(method.getReturnType());
                });
        return (DataSource) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{DataSource.class},
                (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) {
                        return connection;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0F;
        }
        if (returnType == Double.TYPE) {
            return 0D;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return null;
    }

    private static final class DatabaseMetaDataInvocationHandler implements InvocationHandler {

        private final String databaseProductName;

        private DatabaseMetaDataInvocationHandler(String databaseProductName) {
            this.databaseProductName = databaseProductName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getDatabaseProductName".equals(method.getName())) {
                return databaseProductName;
            }
            if (method.getReturnType() == Boolean.TYPE) {
                return Boolean.FALSE;
            }
            if (method.getReturnType() == Byte.TYPE) {
                return (byte) 0;
            }
            if (method.getReturnType() == Short.TYPE) {
                return (short) 0;
            }
            if (method.getReturnType() == Integer.TYPE) {
                return 0;
            }
            if (method.getReturnType() == Long.TYPE) {
                return 0L;
            }
            if (method.getReturnType() == Float.TYPE) {
                return 0F;
            }
            if (method.getReturnType() == Double.TYPE) {
                return 0D;
            }
            if (method.getReturnType() == Character.TYPE) {
                return '\0';
            }
            return null;
        }
    }
}
