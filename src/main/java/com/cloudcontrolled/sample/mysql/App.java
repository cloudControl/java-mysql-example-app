package com.cloudcontrolled.sample.mysql;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;

public class App {

    private static class Write extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Connection connection;
            try {
                connection = getConnection();
                String insert = "INSERT INTO messages (message) VALUES(?)";
                PreparedStatement stmt = connection.prepareStatement(insert);
                stmt.setString(1, req.getParameter("message"));
                stmt.executeUpdate();
                connection.close();
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }
    }

    private static class Read extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Connection connection;
            PrintWriter out = resp.getWriter();
            try {
                connection = getConnection();
                Statement stmt = connection.createStatement();
                String select = "SELECT * FROM messages GROUP BY id DESC LIMIT 1";
                ResultSet rs = stmt.executeQuery(select);
                if (rs.next()) {
                    out.print(rs.getString("message"));
                } else {
                    out.print("Sorry, no message for you!!!");
                }
                out.flush();
                out.close();
                connection.close();
            } catch (SQLException e) {
                throw new ServletException(e);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        createSchema();
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new Write()), "/write");
        context.addServlet(new ServletHolder(new Read()), "/read");
        server.start();
        server.join();
    }

    private static void createSchema() throws SQLException {
        Connection connection = getConnection();
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("DROP TABLE IF EXISTS messages");
        stmt.executeUpdate("CREATE TABLE messages (id INT AUTO_INCREMENT, message VARCHAR(45), PRIMARY KEY (id))");
        connection.close();
    }

    protected static Connection getConnection() throws SQLException {
        String host = System.getenv("MYSQLS_HOSTNAME");
        String port = System.getenv("MYSQLS_PORT");
        String database = System.getenv("MYSQLS_DATABASE");
        String username = System.getenv("MYSQLS_USERNAME");
        String password = System.getenv("MYSQLS_PASSWORD");
        String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" +database;
        return DriverManager.getConnection(dbUrl, username, password);
    }
}
