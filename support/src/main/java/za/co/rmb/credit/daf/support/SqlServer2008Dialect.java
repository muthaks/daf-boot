package za.co.rmb.credit.daf.support;

import org.hibernate.community.dialect.SQLServerLegacyDialect;
import org.hibernate.community.dialect.pagination.SQLServer2005LimitHandler;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.spi.Limit;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * The legacy persistence unit's {@code org.hibernate.dialect.SQLServer2008Dialect}, on Hibernate 7.
 *
 * <p>Hibernate 7's SQLServerDialect pages with {@code ORDER BY ... OFFSET ... FETCH}, adding
 * {@code ORDER BY @@version} to an unordered query; SQL Server rejects that after {@code SELECT DISTINCT},
 * which the legacy workflow list queries use. The legacy dialect paged with {@code TOP} and
 * {@code ROW_NUMBER()} (SQLServer2005LimitHandler). SQLServerLegacyDialect at version 10 (SQL Server 2008)
 * generates SQL the same way. The version is fixed so it does not follow the server's own version.</p>
 */
public class SqlServer2008Dialect extends SQLServerLegacyDialect {

    public SqlServer2008Dialect() {
        super(DatabaseVersion.make(10));
    }

    @Override
    public LimitHandler getLimitHandler() {
        return new TopLimitFix();
    }

    /**
     * SQLServer2005LimitHandler as ported to Hibernate 7 binds {@code TOP(?)} as {@code maxRows - 1} when the
     * query sets a maximum but no first row ({@code setMaxResults} without {@code setFirstResult}): the
     * first row is no longer converted to its 1-based value in that case. {@code setMaxResults(1)} then
     * selects no rows and a page of 20 returns 19. Hibernate 5 bound {@code maxRows}; so does this.
     */
    static final class TopLimitFix extends SQLServer2005LimitHandler {

        @Override
        public int bindLimitParametersAtStartOfQuery(Limit limit, PreparedStatement statement, int index)
                throws SQLException {
            int bound = super.bindLimitParametersAtStartOfQuery(limit, statement, index);
            if (bound == 1 && limit != null && limit.getFirstRow() == null && limit.getMaxRows() != null) {
                statement.setInt(index, limit.getMaxRows());
            }
            return bound;
        }
    }
}
