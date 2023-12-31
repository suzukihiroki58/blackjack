package service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import dao.GameRecordsDAO;
import model.BlackjackGame;
import model.GameRecord;
import model.UserCredential;

public class BlackjackGameFacade {

	public BlackjackGame initializeGame() {
		return new BlackjackGame();
	}

	public enum PlayerAction {
		HIT, STAND, SPLIT
	}

	public void performPlayerAction(BlackjackGame game, PlayerAction action, int handIndex) {
		if (game.getPlayerStateForHand(handIndex) == BlackjackGame.PlayerState.BURST) {
	        return;
	    }

		switch (action) {
		case HIT:
			if (game.getPlayer().getHandTotal(handIndex) == 21) {
		        return;
		    }
			game.playerHit(game.drawCard(), handIndex);
			break;
		case STAND:
			game.playerStand(handIndex);
			break;
		case SPLIT:
			performSplit(game);
			game.addNewHandForSplit();
			break;
		}

		if (game.isGameOver(handIndex)) {
			while (game.getDealer().getHandTotal(0) < 17) {
				if (game.getDealer().getHandTotal(0) > 21) {
					break;
				}
				game.getDealer().receiveCard(game.drawCard(), 0);
			}
		}
	}

	public BlackjackGame getOrCreateGame(HttpSession session, UserCredential loginUser) {
		BlackjackGame game = (BlackjackGame) session.getAttribute("game");

		if (game == null) {
			game = initializeGame();
			GameRecordsDAO gameRecordsDAO = new GameRecordsDAO();
			int chips = gameRecordsDAO.getPlayerChips(loginUser.getUserId());
			game.getPlayer().setChips(chips);
			session.setAttribute("game", game);
		}
		return game;
	}

	public void handlePostRequest(HttpSession session, String action, int handIndex) {
		UserCredential loginUser = getOrCreateLoginUser(session);
		BlackjackGame game = getOrCreateGame(session, loginUser);
		if (action != null) {
			PlayerAction playerAction;
			try {
				playerAction = PlayerAction.valueOf(action.toUpperCase());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				return;
			}
			performPlayerAction(game, playerAction, handIndex);
		}
	}

	public List<String> checkWinners(BlackjackGame game, UserCredential loginUser) {
		List<String> resultMessages = new ArrayList<>();
		int dealerTotal = game.getDealer().getHandTotal(0);

		for (int handIndex = 0; handIndex < game.getPlayer().getHands().size(); handIndex++) {
			int playerTotal = game.getPlayer().getHandTotal(handIndex);
			String resultMessage = calculateResultMessage(playerTotal, dealerTotal);
			BlackjackGame.GameResult gameResult = translateToGameResult(resultMessage);
			game.updateChipsBasedOnOutcome(gameResult, handIndex);
			resultMessages.add(resultMessage);
		}

		GameRecordsDAO gameRecordsDAO = new GameRecordsDAO();
		gameRecordsDAO.updatePlayerChips(loginUser.getUserId(), game.getPlayer().getChips());

		return resultMessages;
	}

	private BlackjackGame.GameResult translateToGameResult(String resultMessage) {
		if (resultMessage.contains("プレイヤーの勝利")) {
			return BlackjackGame.GameResult.PLAYER_WINS;
		} else if (resultMessage.contains("引き分け")) {
			return BlackjackGame.GameResult.DRAW;
		} else {
			return BlackjackGame.GameResult.DEALER_WINS;
		}
	}

	private String calculateResultMessage(int playerTotal, int dealerTotal) {
		boolean win = false;
		boolean lose = false;
		boolean draw = false;

		String resultMessage = "";

		if (playerTotal > 21) {
			lose = true;
			resultMessage = "プレイヤーのバーストにより、ディーラーの勝利";
		} else if (dealerTotal > 21) {
			win = true;
			resultMessage = "ディーラーのバーストにより、プレイヤーの勝利";
		} else if (playerTotal > dealerTotal) {
			win = true;
			resultMessage = "プレイヤーの勝利";
		} else if (dealerTotal > playerTotal) {
			lose = true;
			resultMessage = "ディーラーの勝利";
		} else {
			draw = true;
			resultMessage = "引き分け";
		}
		return resultMessage;
	}

	public String updateGameRecordsAndReturnMessage(UserCredential loginUser, boolean win, boolean lose, boolean draw,
			String resultMessage, int playerTotal, int dealerTotal, int handIndex) {
		try {
			GameRecordsDAO dao = new GameRecordsDAO();
			GameRecord gameRecord = new GameRecord(String.valueOf(loginUser.getUserId()), win ? 1 : 0, lose ? 1 : 0,
					draw ? 1 : 0);
			dao.updateGameRecords(gameRecord);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return resultMessage;
	}

	public UserCredential getOrCreateLoginUser(HttpSession session) {
		UserCredential loginUser = (UserCredential) session.getAttribute("loginUser");
		if (loginUser == null) {
			loginUser = new UserCredential();
			session.setAttribute("loginUser", loginUser);
		}
		return loginUser;
	}

	public void performSplit(BlackjackGame game) {
		game.getPlayer().splitHand();
		game.addNewBetForSplit();
		game.getPlayer().receiveCard(game.drawCard(), 0);
		game.getPlayer().receiveCard(game.drawCard(), 1);
	}
}
