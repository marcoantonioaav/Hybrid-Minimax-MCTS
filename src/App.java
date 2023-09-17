import java.util.ArrayList;
import java.util.List;

import game.Game;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import search.mcts.MCTS;


/**
 * An example of a custom implementation of a match between different AIs,
 * i.e. not using the built-in EvalGamesSet functionality of Ludii.
 * 
 * By creating custom implementations of experiments, we can more
 * easily add our own custom stats to track, and request moves from AIs
 * that do not implement Ludii's abstract AI class. The downside is that
 * more boilerplate code must be written.
 * 
 * See RunLudiiMatch for an example that uses Ludii's built-in EvalGamesSet 
 * implementation.
 * 
 * Note that this example does not provide all of the functionality included
 * in the built-in EvalGamesSet implementation. For instance, this example will always
 * use the same agents for the same player number (e.g. Random AI always player
 * 1, UCT always player 2), whereas Ludii's built-in EvalGamesSet implementation can
 * rotate through assignments of agents to player numbers.
 * 
 * @author Dennis Soemers
 */
public class App {
	static final HybridMinimaxMCTS HYBRID_MINIMAX_MCTS = new HybridMinimaxMCTS();
	static final List<String> GAME_NAMES = List.of("Tic-tac-toe.lud", "Tapatan.lud", "Alquerque.lud", "Reversi.lud");
	static final int MAX_STEPS = 100;
	static final int NUM_GAMES = 20;
	static final double THINKING_TIME = 5.0;

	private App() {}

	public static void main(final String[] args) {
		HYBRID_MINIMAX_MCTS.setEvaluationPlayouts(15);
		HYBRID_MINIMAX_MCTS.setMaxPlayoutDepth(100);
		for(String gameName : GAME_NAMES) {
			final Game game = GameLoader.loadGameFromName(gameName);

			final Trial trial = new Trial(game);
			final Context context = new Context(game, trial);
			
			for (int gameCounter = 0; gameCounter < NUM_GAMES; ++gameCounter)
			{
				game.start(context);
				System.out.println("Game : " + game.name());

				final List<AI> ais = getAIs(gameCounter);
				for (int player = 1; player < ais.size(); ++player)
				{
					System.out.println("Player " + player + " : " + ais.get(player).friendlyName());
					ais.get(player).initAI(game, player);
				}
				
				int steps = 0;

				final Model model = context.model();

				while (!context.trial().over() && steps <= MAX_STEPS)
				{
					model.startNewStep(context, ais, THINKING_TIME);
					steps++;
				}
				
				System.out.println("Outcome = " + context.trial().status());
				System.out.println("First reached depth = " + HYBRID_MINIMAX_MCTS.getFirstReachedDepth());
				System.out.println("Mean reached depth = " + HYBRID_MINIMAX_MCTS.getMeanReachedDepth());
				System.out.println("Mean spent time = " + HYBRID_MINIMAX_MCTS.getMeanSpentTimeSeconds() + "s");
				System.out.println("===============================");
			}
		}
	}

	private static List<AI> getAIs(int gameCounter) {
		List<AI> ais = new ArrayList<>();
		ais.add(null);
		ais.add(null);
		ais.add(null);
		ais.set(gameCounter%2 +1, HYBRID_MINIMAX_MCTS);
		ais.set((1 - gameCounter%2) + 1, MCTS.createUCT()); // built-in Ludii UCT
		return ais;
	}
}