package com.dota2.api.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class DuckDBTypeHandlerConfig {

    @MappedTypes(LocalDateTime.class)
    public static class DuckDBLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType) throws SQLException {
            ps.setTimestamp(i, Timestamp.valueOf(parameter));
        }

        @Override
        public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
            Timestamp ts = rs.getTimestamp(columnName);
            return ts == null ? null : ts.toLocalDateTime();
        }

        @Override
        public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            Timestamp ts = rs.getTimestamp(columnIndex);
            return ts == null ? null : ts.toLocalDateTime();
        }

        @Override
        public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            Timestamp ts = cs.getTimestamp(columnIndex);
            return ts == null ? null : ts.toLocalDateTime();
        }
    }

    @MappedTypes(LocalDate.class)
    public static class DuckDBLocalDateTypeHandler extends BaseTypeHandler<LocalDate> {
        @Override
        public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType) throws SQLException {
            ps.setDate(i, Date.valueOf(parameter));
        }

        @Override
        public LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
            Date d = rs.getDate(columnName);
            return d == null ? null : d.toLocalDate();
        }

        @Override
        public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            Date d = rs.getDate(columnIndex);
            return d == null ? null : d.toLocalDate();
        }

        @Override
        public LocalDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
            Date d = cs.getDate(columnIndex);
            return d == null ? null : d.toLocalDate();
        }
    }

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            configuration.getTypeHandlerRegistry().register(LocalDateTime.class, new DuckDBLocalDateTimeTypeHandler());
            configuration.getTypeHandlerRegistry().register(LocalDate.class, new DuckDBLocalDateTypeHandler());
        };
    }
}
