package com.karuslabs.jdbi.bug.report;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.fail;

record Foo(UUID id, UUID next, long createdAt, long updatedAt) {}

public class FailingTest {
    
    Jdbi jdbi;
    
    @BeforeEach
    void before() {
        var container = new PostgreSQLContainer<>("postgres:latest").withInitScript("init.sql");
        container.start();
        
        var source = new HikariDataSource();
        source.setJdbcUrl(container.getJdbcUrl());
        source.setUsername(container.getUsername());
        source.setPassword(container.getPassword());
        
        jdbi = Jdbi.create(source).installPlugin(new PostgresPlugin());
    }
    
    @Test
    void failing() {
        var first = UUID.randomUUID();
        var second = UUID.randomUUID();

        var foos = List.of(
            new Foo(first, second, 0, 2),
            new Foo(second, null, 0, 2)
        );
        
        jdbi.useTransaction(transaction -> {
            var batch = transaction.prepareBatch("""
                INSERT INTO foo (id, next, created_at, updated_at) VALUES (:id, :next, :created_at, :updated_at)
            """);
            
            for (var foo : foos) {
                batch.bind("id", foo.id())
                     .bindBySqlType("next", foo.next(), Types.OTHER)
                     .bind("created_at", new Timestamp(foo.createdAt()))
                     .bind("updated_at", new Timestamp(foo.updatedAt()))
                     .add();
            }
            
            batch.execute();
        });
        
        jdbi.useTransaction(transaction -> {
            var results = transaction.createQuery("SELECT * FROM foo").mapToMap().list();
            System.out.println("Foos: " + results.size());
            for (var row : results) {
                System.out.println(row);
            }
        });
        
        fail();
    }
    
}
