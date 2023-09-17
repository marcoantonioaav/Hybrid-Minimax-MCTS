import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import game.Game;
import main.collections.FastArrayList;
import other.AI;
import other.context.Context;
import other.move.Move;
import utils.AIUtils;

public class HybridMinimaxMCTS extends AI {
    protected int player = -1;
    private Game game;

    private int evaluationPlayouts = 25;
    private int maxPlayoutDepth = 50;

    private List<Integer> reachedDepths = new ArrayList<>();
    private List<Long> spentTimes = new ArrayList<>();

    public static final float MAX = 1;
    public static final float MIN = -MAX;
    public static final float NEUTRAL = (MIN + MAX)/2;

    public HybridMinimaxMCTS() {
        this.friendlyName = "Hybrid Minimax-MCTS";
    }

    public void setEvaluationPlayouts(int evaluationPlayouts) {
        this.evaluationPlayouts = evaluationPlayouts;
    }

    public void setMaxPlayoutDepth(int maxPlayoutDepth) {
        this.maxPlayoutDepth = maxPlayoutDepth;
    }

    public int getFirstReachedDepth() {
        return reachedDepths.get(0);
    }

    public int getMeanReachedDepth() {
        int reachedDepthSum = 0;
        for(int reachedDepth : reachedDepths) {
            reachedDepthSum += reachedDepth;
        }
        return reachedDepthSum/reachedDepths.size();
    }

    public float getMeanSpentTimeSeconds() {
        long spentTimeSum = 0;
        for(long spentTime : spentTimes) {
            spentTimeSum += spentTime;
        }
        return (spentTimeSum/spentTimes.size())/1000f;
    }

    @Override
    public Move selectAction(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth) {
        FastArrayList<Move> legalMoves = game.moves(context).moves();
        if (!game.isAlternatingMoveGame())
            legalMoves = AIUtils.extractMovesForMover(legalMoves, player);

        Move bestMove = legalMoves.get(0);
        float bestScore = MIN;

        final long SEARCH_TIME_MILLIS = (long)(maxSeconds*1000);
        long startTime = System.currentTimeMillis();
        long timeSpent = 0;
        long iterationTimeSpent = 0;
        long lastIterationTimeSpent = 0;
        int depth = 0;
        while(timeSpent + iterationTimeSpent + (iterationTimeSpent - lastIterationTimeSpent) <= SEARCH_TIME_MILLIS) {
            lastIterationTimeSpent = iterationTimeSpent;
            long iterationStartTime = System.currentTimeMillis();
            bestMove = legalMoves.get(0);
            bestScore = MIN;
            for(Move move : legalMoves) {
                Context newContext = new Context(context);
                newContext.game().apply(newContext, move);
                float score = minimax(newContext, depth, MIN, MAX, false);
                if(score > bestScore) {
                    bestMove = move;
                    bestScore = score;
                }
            }
            depth++;
            iterationTimeSpent = System.currentTimeMillis() - iterationStartTime;
            timeSpent = System.currentTimeMillis() - startTime;
        }
        reachedDepths.add(depth);
        spentTimes.add(timeSpent);
        return bestMove;
    }

    private float minimax(Context context, int depth, float alpha, float beta, boolean isMaximizing) {
        if(depth == 0 || context.trial().over())
            return evaluate(context, isMaximizing);
        FastArrayList<Move> legalMoves = game.moves(context).moves();
        if (!game.isAlternatingMoveGame())
            legalMoves = AIUtils.extractMovesForMover(legalMoves, player);
        if(isMaximizing) {
            float maxValue = MIN;
            for(Move move : legalMoves) {
                Context newContext = new Context(context);
				newContext.game().apply(newContext, move);
                float newValue = minimax(newContext, depth - 1, alpha, beta, false);
                maxValue = Math.max(maxValue, newValue);
                if(maxValue >= beta)
                    break;
                alpha = Math.max(alpha, maxValue);
            }
            return maxValue;
        }
        else {
            float minValue = MAX;
            for(Move move : legalMoves) {
                Context newContext = new Context(context);
                newContext.game().apply(newContext, move);
                float newValue = minimax(newContext, depth - 1, alpha, beta, true);
                minValue = Math.min(minValue, newValue);
                if(minValue <= alpha)
                    break;
                beta = Math.min(beta, minValue);
            }
            return minValue;
        }
    }
    private float evaluate(Context context, boolean isMaximizing) {
        if(context.trial().over())
            return evaluateTerminalState(context);
        int startingPlayer = player;
        if(!isMaximizing)
            startingPlayer = 1 - player;
        return evaluateWithPlayouts(context, startingPlayer);
    }

    private float evaluateTerminalState(Context context) {
        if(context.winners().contains(this.player))
            return MAX;
        if(!context.winners().isEmpty())
            return MIN;
        return NEUTRAL;
    }

    private float evaluateWithPlayouts(Context context, int startingPlayer) {
        float evaluation = 0f;
        for(int p = 0; p < evaluationPlayouts; p++)
            evaluation += makePlayout(context, startingPlayer);
        return evaluation/evaluationPlayouts;
    }

    private float makePlayout(Context context, int startingPlayer) {
		Context newContext = new Context(context);
		int currentPlayer = startingPlayer;
        int depth = 0;
		while(!newContext.trial().over() && depth < maxPlayoutDepth) {
			Move move = getRandomMove(newContext, currentPlayer);
			newContext.game().apply(newContext, move);
			currentPlayer = 1 - currentPlayer;
            depth++;
		}
		return evaluateTerminalState(newContext);
	}

    private Move getRandomMove(Context context, int player) {
		FastArrayList<Move> legalMoves = game.moves(context).moves();
		
		if (!game.isAlternatingMoveGame())
			legalMoves = AIUtils.extractMovesForMover(legalMoves, player);
		
		final int r = ThreadLocalRandom.current().nextInt(legalMoves.size());
		return legalMoves.get(r);
	}

    @Override
	public void initAI(final Game game, final int playerID)
	{
        this.game = game;
		this.player = playerID;
        this.reachedDepths.clear();
        this.spentTimes.clear();
	}
}
