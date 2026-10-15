package za.co.rmb.approvalforum.data;

import jakarta.inject.Singleton;

import jakarta.inject.Inject;
import javax.sql.DataSource;

@Singleton
class ApprovalForumPropertiesProvider extends ApprovalForumJdbcMapProvider {

    private static final String SQL = "SELECT name AS [key], value AS value FROM RM_ListProperties";

    @Inject
    public ApprovalForumPropertiesProvider(DataSource ds) {
        super(ds, SQL);
    }
}
