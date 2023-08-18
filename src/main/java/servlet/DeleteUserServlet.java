package servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import dao.AccountsDAO;
import model.Login;

@WebServlet("/DeleteUserServlet")
public class DeleteUserServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		if (session != null) {
			Login loggedInUser = (Login) session.getAttribute("loginUser");

			if (loggedInUser != null) {
				AccountsDAO dbManager = new AccountsDAO();
				dbManager.deleteUser(loggedInUser.getUsername());

				session.invalidate();
				response.sendRedirect("WelcomeServlet?message=UserDeleted");
				return;
			}
		}
		response.sendRedirect("AdminServlet");
	}
}
