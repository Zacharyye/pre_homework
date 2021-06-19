package com.zachary.pre_homework02_02;

import java.io.*;
import java.sql.Connection;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import javax.sql.DataSource;

@WebServlet(name = "helloServlet", value = "/hello-servlet")
public class HelloServlet extends HttpServlet {
    private String message;

    public void init() {
        message = "Hello World!";
        getConnectionFromJDNI();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");

        // Hello
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>" + message + "</h1>");
        out.println("</body></html>");
    }

    public void destroy() {
    }

    public Connection getConnectionFromJDNI () {
        Connection connection = null;
        try {
            long t = System.currentTimeMillis();
            Context initCtx = new InitialContext();
            DataSource ds = (DataSource) initCtx.lookup("java:comp/env/jdbc/UserPlatformDataSource");
            connection = ds.getConnection();
            t = System.currentTimeMillis() - t;
            System.out.println("耗时: " + t);
            if(connection != null) {
                System.out.println("连接成功啦~");
            }
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}