package com.votantai.transactionrisk.auth.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.votantai.transactionrisk.auth.application.UsernameNormalizer;
import com.votantai.transactionrisk.auth.domain.Role;
import com.votantai.transactionrisk.auth.domain.UserAccount;

@Repository
public class UserRepository {

    private static final String SELECT_COLUMNS = "id, username, password_hash, role, enabled, created_at, updated_at";

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findByUsername(String username) {
        return jdbcTemplate.query("SELECT " + SELECT_COLUMNS + " FROM users WHERE username = ?",
                        this::mapUser, UsernameNormalizer.normalize(username))
                .stream().findFirst();
    }

    public Optional<UserAccount> findById(UUID id) {
        return jdbcTemplate.query("SELECT " + SELECT_COLUMNS + " FROM users WHERE id = ?",
                        this::mapUser, id)
                .stream().findFirst();
    }

    public void insertIfAbsent(UUID id, String username, String passwordHash, Role role) {
        jdbcTemplate.update("""
                INSERT INTO users (id, username, password_hash, role, enabled)
                VALUES (?, ?, ?, ?, TRUE)
                ON CONFLICT (username) DO NOTHING
                """, id, UsernameNormalizer.normalize(username), passwordHash, role.name());
    }

    private UserAccount mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UserAccount(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                Role.valueOf(resultSet.getString("role")),
                resultSet.getBoolean("enabled"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}
