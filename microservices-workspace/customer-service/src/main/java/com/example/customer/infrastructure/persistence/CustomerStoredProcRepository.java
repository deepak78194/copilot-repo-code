package com.example.customer.infrastructure.persistence;

import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class CustomerStoredProcRepository {

    private final DataSource dataSource;

    public CustomerStoredProcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CustomerRow createCustomer(String firstName, String lastName, String email, String phone) {
        String sql = "{call create_customer(?, ?, ?, ?, ?)}";
        try (Connection connection = dataSource.getConnection();
             CallableStatement stmt = connection.prepareCall(sql)) {

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.registerOutParameter(5, Types.NUMERIC);

            stmt.execute();

            long generatedId = stmt.getLong(5);
            return new CustomerRow(generatedId, firstName, lastName, email, phone, LocalDateTime.now());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to call create_customer stored procedure", e);
        }
    }

    public Optional<CustomerRow> findById(Long id) {
        String sql = "{call get_customer_by_id(?, ?)}";
        try (Connection connection = dataSource.getConnection();
             CallableStatement stmt = connection.prepareCall(sql)) {

            stmt.setLong(1, id);
            stmt.registerOutParameter(2, Types.REF_CURSOR);

            stmt.execute();

            try (ResultSet rs = (ResultSet) stmt.getObject(2)) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to call get_customer_by_id stored procedure", e);
        }
    }

    public List<CustomerRow> findAll() {
        String sql = "{call get_all_customers(?)}";
        try (Connection connection = dataSource.getConnection();
             CallableStatement stmt = connection.prepareCall(sql)) {

            stmt.registerOutParameter(1, Types.REF_CURSOR);
            stmt.execute();

            List<CustomerRow> customers = new ArrayList<>();
            try (ResultSet rs = (ResultSet) stmt.getObject(1)) {
                while (rs.next()) {
                    customers.add(mapRow(rs));
                }
            }
            return customers;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to call get_all_customers stored procedure", e);
        }
    }

    private CustomerRow mapRow(ResultSet rs) throws SQLException {
        return new CustomerRow(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("phone"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    public record CustomerRow(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDateTime createdAt
    ) {
    }
}
