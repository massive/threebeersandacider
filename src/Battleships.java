//
// Compiling:
// javac -classpath .:lib/json-simple-1.1.1.jar src/Battleships.java
//
// Starting the server:
// java -classpath .:lib/json-simple-1.1.1.jar:src/ Battleships
//

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Random;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Battleships {

    public static void main(String[] args) throws Exception {
        initializeGrid();
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        System.out.println("Running at " + server.getAddress());

        server.createContext("/start_game", new StartGameHandler());
        server.createContext("/next_turn", new NextTurnHandler());
        server.createContext("/end_game", new EndGameHandler());

        server.start();
    }

    static abstract class HandlerWithStringResponse implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            Object parsed;

            if (t.getRequestMethod().equals("POST")) {
                InputStream bodyStream = t.getRequestBody();
                String requestBody = convertStreamToString(bodyStream);

                try {
                    parsed = new JSONParser().parse(requestBody);
                } catch (ParseException e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                parsed = null;
            }

            String response = getResponse((JSONObject) parsed);
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();

            os.write(response.getBytes());
            os.close();
        }

        protected abstract String getResponse(JSONObject params);
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    static class StartGameHandler extends HandlerWithStringResponse {
        @Override
        protected String getResponse(JSONObject params) {
            initializeGrid();
            return "{ grid:\n" +
                "   [ [ '1', ' ', '2', '2', '2', '2', ' ', ' ', ' ', ' ' ],\n" +
                "     [ '1', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' ],\n" +
                "     [ '1', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' ],\n" +
                "     [ '1', ' ', '3', ' ', ' ', ' ', ' ', ' ', ' ', ' ' ],\n" +
                "     [ '1', ' ', '3', ' ', ' ', ' ', ' ', ' ', ' ', ' ' ],\n" +
                "     [ ' ', ' ', '3', ' ', ' ', ' ', ' ', ' ', ' ', ' ' ],\n" +
                "     [ ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' ],\n" +
                "     [ ' ', ' ', ' ', ' ', '4', '4', '4', ' ', ' ', ' ' ],\n" +
                "     [ ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '5' ],\n" +
                "     [ ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', '5' ] ]\n" +
                "}\n";
        }
    }

    static class NextTurnHandler extends HandlerWithStringResponse {

        @Override
        protected String getResponse(JSONObject params) {
            System.out.println("got " + params.get("name"));
            int[] target = null;
            while (target == null) {
                System.out.println("Calculating target...");
                try {
                    target = calculateTarget();
                } catch (Exception e) {
                    System.err.println("Error: " + e + ". Trying again...");
                }
            }
            System.out.println("Target: " + target[0] + ", " + target[1]);
            return "{\n" +
                "  x: " + target[0] + ",\n" +
                "  y: " + target[1] + "\n" +
                "}\n";
        }
    }

    static class EndGameHandler extends HandlerWithStringResponse {
        @Override
        protected String getResponse(JSONObject params) {
            return "{\n" +
                "  \"message\" : \"AAAGH!\"\n" +
                "}\n";
        }
    }

    private static final int SIZE_X = 10;
    private static final int SIZE_Y = 10;

    private enum State {
        UNKNOWN, SHIP, NO_SHIP
    };

    private static State[][] grid = new State[SIZE_X][SIZE_Y];

    private static Random random = new Random();

    private static void initializeGrid() {
        for (int y = 0; y < SIZE_Y; y++) {
            for (int x = 0; x < SIZE_X; x++) {
                grid[x][y] = State.UNKNOWN;
            }
        }
    }

    private static int[] calculateTarget() {
        int[] randomTarget;
        while (true) {
            randomTarget = getRandomTarget();
            if (getState(randomTarget) == State.UNKNOWN) break;
        }
        setState(randomTarget, State.NO_SHIP);
        return randomTarget;
    }

    private static State getState(int[] target) {
        return grid[target[0]][target[1]];
    }

    private static void setState(int[] target, State setTo) {
        grid[target[0]][target[1]] = setTo;
    }

    private static int[] getRandomTarget() {
        return new int[]{Math.abs(random.nextInt()) % SIZE_X, Math.abs(random.nextInt()) % SIZE_Y};
    }
}
