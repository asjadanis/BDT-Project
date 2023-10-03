package query;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SimpleImpalaQuery {
    public static void main(String[] args) {
        String JDBC_DRIVER = "org.apache.hive.jdbc.HiveDriver";
        String CONNECTION_URL = "jdbc:hive2://localhost:21050/test_db;auth=noSasl"; // Update hostname, port, and database name

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // Register JDBC Driver
            Class.forName(JDBC_DRIVER);

            // Open a connection
            System.out.println("Connecting to Impala...");
            conn = DriverManager.getConnection(CONNECTION_URL);

            // Execute queries
            stmt = conn.createStatement();

            // Query 1: Get first 5 records
            String sql1 = "SELECT * FROM employee_table LIMIT 5";
            rs = stmt.executeQuery(sql1);
            while (rs.next()) {
                int index = rs.getInt("Index");
                String userId = rs.getString("User_Id");
                System.out.println("Index: " + index + ", User Id: " + userId);
            }
            
            rs.close();

            // Query 2: Count males and females
            String sql2 = "SELECT Sex, COUNT(*) FROM employee_table GROUP BY Sex";
            rs = stmt.executeQuery(sql2);
            while (rs.next()) {
                String sex = rs.getString("Sex");
                int count = rs.getInt("COUNT(*)");
                System.out.println("Sex: " + sex + ", Count: " + count);
            }

            // Query 3: Get the top 3 most frequent job titles
            String sql3 = "SELECT Job_Title, COUNT(*) as Count FROM employee_table GROUP BY Job_Title ORDER BY Count DESC LIMIT 3";
            rs = stmt.executeQuery(sql3);
            while (rs.next()) {
                String jobTitle = rs.getString("Job_Title");
                int count = rs.getInt("Count");
                System.out.println("Job Title: " + jobTitle + ", Count: " + count);
            }

            rs.close();

            // Query 4: Get the number of users with the same first name
            String sql4 = "SELECT First_Name, COUNT(*) as Count FROM employee_table GROUP BY First_Name HAVING Count > 1 ORDER BY Count DESC";
            rs = stmt.executeQuery(sql4);
            while (rs.next()) {
                String firstName = rs.getString("First_Name");
                int count = rs.getInt("Count");
                System.out.println("First Name: " + firstName + ", Count: " + count);
            }
            rs.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
