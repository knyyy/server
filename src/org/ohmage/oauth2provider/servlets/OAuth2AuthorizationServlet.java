package org.ohmage.oauth2provider.servlets;

import org.ohmage.oauth2provider.beans.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * created by Faisal on 3/19/13 5:49 AM
 */
public class OAuth2AuthorizationServlet extends HttpServlet {
    private ApplicationContext appContext = null;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // attempt to find our test bean
        if (appContext == null) {
            appContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
        }

        TestBean myBean = (TestBean)appContext.getBean("testbean");

        request.getRequestDispatcher("/oauth2/access.jsp")
                .forward(request, response);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        out.println("Hello, authorization! POST");
    }
}
