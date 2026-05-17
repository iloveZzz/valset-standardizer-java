package com.yss.valset.task.infrastructure.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.io.IOException;
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 通用 CLOB/String 类型处理器，兼容 Oracle CLOB 与 MySQL/PostgreSQL 普通字符串列读取。
 */
@MappedTypes(String.class)
@MappedJdbcTypes({JdbcType.CLOB, JdbcType.LONGVARCHAR, JdbcType.VARCHAR})
public class ClobToStringTypeHandler extends BaseTypeHandler<String> {

    /**
     * 写入字符串参数。
     *
     * @param ps JDBC 预编译语句
     * @param i 参数索引
     * @param parameter 参数值
     * @param jdbcType JDBC 类型
     * @throws SQLException JDBC 异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter);
    }

    /**
     * 按列名读取字符串结果。
     *
     * @param rs 结果集
     * @param columnName 列名
     * @return 读取后的字符串
     * @throws SQLException JDBC 异常
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return readValue(rs.getObject(columnName));
    }

    /**
     * 按列索引读取字符串结果。
     *
     * @param rs 结果集
     * @param columnIndex 列索引
     * @return 读取后的字符串
     * @throws SQLException JDBC 异常
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return readValue(rs.getObject(columnIndex));
    }

    /**
     * 从存储过程结果中读取字符串。
     *
     * @param cs 可调用语句
     * @param columnIndex 列索引
     * @return 读取后的字符串
     * @throws SQLException JDBC 异常
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return readValue(cs.getObject(columnIndex));
    }

    /**
     * 将 JDBC 返回对象统一转换为字符串。
     *
     * @param value JDBC 返回值
     * @return 字符串结果
     * @throws SQLException JDBC 异常
     */
    private String readValue(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Clob) {
            return readClob((Clob) value);
        }
        return String.valueOf(value);
    }

    /**
     * 完整读取 CLOB 内容。
     *
     * @param clob CLOB 对象
     * @return 字符串内容
     * @throws SQLException JDBC 异常
     */
    private String readClob(Clob clob) throws SQLException {
        if (clob == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[2048];
        try (Reader reader = clob.getCharacterStream()) {
            int length;
            while ((length = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, length);
            }
            return builder.toString();
        } catch (IOException ex) {
            throw new SQLException("读取 CLOB 字段失败", ex);
        }
    }
}
