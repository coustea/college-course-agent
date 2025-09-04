package com.ccut.common.Utils;

import com.ccut.common.entity.Course;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(Course.Status.class)
public class CourseStatusTypeHandler extends BaseTypeHandler<Course.Status> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Course.Status parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public Course.Status getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull()? null : Course.Status.fromCode(code);
    }

    @Override
    public Course.Status getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull()? null : Course.Status.fromCode(code);
    }

    @Override
    public Course.Status getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull()? null : Course.Status.fromCode(code);
    }
}