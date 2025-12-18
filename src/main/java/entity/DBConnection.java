package entity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private final String Url = "jdbc:postgresql://localhost:5432/mini_football_db";
    private final String User = "mini_football_db_manager";
    private final String Password = "123";

    public Connection getDBConnection() {
        try {
            return DriverManager.getConnection(Url, User, Password);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }
}
