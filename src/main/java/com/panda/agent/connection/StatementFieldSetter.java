package com.panda.agent.connection;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface StatementFieldSetter {

    void setField(PreparedStatement statement, int parameterIndex, Object value) throws SQLException;

}
