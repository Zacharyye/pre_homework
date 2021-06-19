package com.zachary.pre_homework02;

import java.io.*;
import java.sql.*;
import javax.annotation.Resource;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import javax.sql.DataSource;

@WebServlet(name = "helloServlet", value = "/hello-servlet")
public class HelloServlet extends HttpServlet {
  private String message;

  @Resource(name="jdbc/UserPlatformDataSource")
  private DataSource dataSource;

  @Override
  public void init() {
    message = "Hello World!";
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");

    //创建相应变量并赋值null
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;

    try {
      System.out.println(dataSource);
      conn = dataSource.getConnection();
      Class.forName("com.mysql.jdbc.Driver");
      conn = DriverManager.getConnection("jdbc:mysql://1.116.154.131:3306/zachary","root", "root");
      stmt = conn.createStatement();
      rs = stmt.executeQuery("select 1 from dual");
      System.out.println(rs);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        conn.close();
      } catch (SQLException throwables) {
        throwables.printStackTrace();
      }
    }


    // Hello
    PrintWriter out = response.getWriter();
    out.println("<html><body>");
    out.println("<h1>" + message + "</h1>");
    out.println("</body></html>");
  }

  @Override
  public void destroy() {
  }
}