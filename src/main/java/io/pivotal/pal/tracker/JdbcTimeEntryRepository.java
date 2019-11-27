package io.pivotal.pal.tracker;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import static java.sql.Statement.RETURN_GENERATED_KEYS;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;


public class JdbcTimeEntryRepository implements TimeEntryRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public JdbcTimeEntryRepository(MysqlDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public TimeEntry create(TimeEntry timeEntry) {
        KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO time_entries (project_id, user_id, date, hours) " +
                            "VALUES (?, ?, ?, ?)",
                    RETURN_GENERATED_KEYS
            );
            statement.setLong(1, timeEntry.getProjectId());
            statement.setLong(2, timeEntry.getUserId());
            statement.setDate(3, Date.valueOf(timeEntry.getDate()));
            statement.setInt(4, timeEntry.getHours());
            return statement;
        }, generatedKeyHolder);
        return find(generatedKeyHolder.getKey().longValue());
    }

    @Override
    public TimeEntry find(long Id) {
        return jdbcTemplate.query(
                "SELECT id, project_id, user_id, date, hours FROM time_entries WHERE id = ?",
                new Object[]{Id},
                extractor);
    }

    @Override
    public TimeEntry update(long Id, TimeEntry timeEntry) {
        jdbcTemplate.update(
                    "UPDATE time_entries " +
                    "SET project_id = ?, user_id = ?, date = ?,  hours = ? " +
                            "WHERE id = ?",
                    timeEntry.getProjectId(),
                    timeEntry.getUserId(),
                    Date.valueOf(timeEntry.getDate()),
                    timeEntry.getHours(),
                    Id
            );
        return find(Id);
    }

    @Override
    public TimeEntry delete(long Id) {
        TimeEntry deleted = find(Id);
        jdbcTemplate.update("DELETE FROM time_entries " +
                "WHERE id = ?", Id);
        return deleted;
    }

    @Override
    public List<TimeEntry> list() {
        return jdbcTemplate.query("SELECT id, project_id, user_id, date, hours FROM time_entries",mapper);

    }
    private final RowMapper<TimeEntry> mapper = (rs, rowNum) -> new TimeEntry(
            rs.getLong("id"),
            rs.getLong("project_id"),
            rs.getLong("user_id"),
            rs.getDate("date").toLocalDate(),
            rs.getInt("hours")
    );

    private final ResultSetExtractor<TimeEntry> extractor =
            (rs) -> rs.next() ? mapper.mapRow(rs, 1) : null;
}
