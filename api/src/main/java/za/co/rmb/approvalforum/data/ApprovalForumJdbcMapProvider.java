package za.co.rmb.approvalforum.data;

import com.google.common.base.Throwables;

import jakarta.inject.Provider;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

abstract class ApprovalForumJdbcMapProvider implements Provider<Map<String, String>> {

    private final DataSource ds;
    private final String sql;

    ApprovalForumJdbcMapProvider(DataSource ds, String sql) {
        this.ds = ds;
        this.sql = sql;
    }

    @Override
    public Map<String, String> get() {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet res = stmt.executeQuery()) {
            Map<String, String> map = new HashMap<>();
            while (res.next()) {
                map.put(res.getString("key"), res.getString("value"));
            }
            return map;
        } catch (SQLException sqle) {
            throw Throwables.propagate(sqle);
        }
    }
}
