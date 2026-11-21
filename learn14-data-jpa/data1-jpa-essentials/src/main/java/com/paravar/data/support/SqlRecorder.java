package com.paravar.data.support;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;

// sees every SQL statement Hibernate sends, so each endpoint can report what it cost
public class SqlRecorder implements StatementInspector {

    private static final ThreadLocal<List<String>> STATEMENTS = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public String inspect(String sql) {
        STATEMENTS.get().add(sql);
        return sql;
    }

    public static void start() {
        STATEMENTS.get().clear();
    }

    public static List<String> statements() {
        return List.copyOf(STATEMENTS.get());
    }
}
