package servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import dao.UsersDAO;
import model.Account;

@WebServlet("/RegisterServlet")
public class RegisterServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		RequestDispatcher dispatcher = request.getRequestDispatcher("WEB-INF/register.jsp");
		dispatcher.forward(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");

		String userName = request.getParameter("userName");
		String password = request.getParameter("password");
		String nickname = request.getParameter("nickname");

		System.out.println("Username: " + userName);
		System.out.println("Password: " + password);
		System.out.println("Nickname: " + nickname);

		UsersDAO accountsDAO = new UsersDAO();

		boolean userNameExists = accountsDAO.isUserNameExists(userName);

		if (userNameExists) {
			request.setAttribute("errorMessage", "そのユーザー名は既に使用されています。");
			RequestDispatcher dispatcher = request.getRequestDispatcher("WEB-INF/register.jsp");
			dispatcher.forward(request, response);
			return;
		}
		
		Account newAccount = new Account(userName, password, nickname);

		boolean isRegistered = accountsDAO.isUserRegisteredSuccessfully(newAccount);

		if (isRegistered) {
			HttpSession session = request.getSession();
			session.setAttribute("userName", userName);
			RequestDispatcher dispatcher = request.getRequestDispatcher("WEB-INF/login.jsp");
			dispatcher.forward(request, response);
		} else {
			response.sendRedirect("RegisterServlet");
		}
	}
}
