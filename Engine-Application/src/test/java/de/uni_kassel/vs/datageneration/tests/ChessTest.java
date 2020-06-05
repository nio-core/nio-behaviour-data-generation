package de.uni_kassel.vs.datageneration.tests;

import de.uni_kassel.vs.datageneration.GameType;
import de.uni_kassel.vs.datageneration.IController;
import de.uni_kassel.vs.datageneration.engines.GameBoard;
import de.uni_kassel.vs.datageneration.engines.EngineController;
import de.uni_kassel.vs.datageneration.engines.commands.Command;
import de.uni_kassel.vs.datageneration.engines.commands.Response;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.regex.Pattern;

public class ChessTest implements IController, GameBoard.CheckMateListener {

    @Test
    public void start() {
        GameBoard gameBoard = new GameBoard("startpos", GameType.Chess, this);
        EngineController testEngineController = EngineController.getFromString("pulsr", GameType.Chess, gameBoard);

        Assert.assertNotNull("TestEngine should not be null", testEngineController);
        Assert.assertEquals("Wrong type", "pulse", testEngineController.getType().name());
        Assert.assertEquals("Wrong Language", EngineController.EngineType.Language.java, testEngineController.getType().getLang());

        try {
            testEngineController.startProcess();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }

        Response response = null;
        try {
            // start engine
            response = testEngineController.sendCommand(Command.start);
            Assert.assertNull(response);

            // check status
            response = testEngineController.sendCommand(Command.isready);
            Assert.assertNull(response);


            // init new game
            response = testEngineController.sendCommand(Command.newgame);
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertNull(response);

        Random random = new Random();
        int millis;

        try {
            // whites turn
            millis = random.nextInt(4001) + 1000;
            Assert.assertTrue("", millis <= 5000 && millis >= 1000);

            response = testEngineController.sendCommand(Command.position.setBoardPosition(gameBoard.getStartPos()).setMoves(gameBoard.getMovesAsString()));
            Assert.assertNull(response);

            response = testEngineController.sendCommand(Command.search);
            Assert.assertNull(response);

            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            response = testEngineController.sendCommand(Command.stop);
            Assert.assertNotNull(response);
            Assert.assertTrue("Response not right (from)", Pattern.matches("[a-h][1-8]", response.getFrom()));
            Assert.assertTrue("Response not right (to)", Pattern.matches("[a-h][1-8]", response.getTo()));

            gameBoard.addMove(response, 'B');
            response = testEngineController.sendCommand(Command.position.setBoardPosition(gameBoard.getStartPos()).setMoves(gameBoard.getMovesAsString()));
            Assert.assertNull(response);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    @Override
    public void alertCheckMate(boolean giveUp) {

    }

    @Override
    public void quitWithError() {
        Assert.fail();
    }
}