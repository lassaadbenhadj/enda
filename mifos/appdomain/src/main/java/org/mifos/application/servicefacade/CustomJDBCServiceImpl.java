package org.mifos.application.servicefacade;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class CustomJDBCServiceImpl implements CustomJDBCService {

    private final JdbcTemplate template;

    @Autowired
    public CustomJDBCServiceImpl(final DataSource dataSource) {
        template = new JdbcTemplate(dataSource);
    }

    @Override
    public boolean mifos4948IssueKeyExists() {

        int count = this.template
                .queryForInt("select count(*) from config_key_value_integer c where c.configuration_key like 'MIFOS-4948'");

        return count > 0;
    }

    @Override
    public void insertMifos4948Issuekey() {
        this.template
                .execute("insert into config_key_value_integer (configuration_key, configuration_value) values ('MIFOS-4948', 1)");
    }
}