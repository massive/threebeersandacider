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
import java.util.*;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;
import jdk.nashorn.internal.ir.debug.JSONWriter;
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
            try {
                return getNewGrid();
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }
    }

    private static String getNewGrid()
    {
        String[][] grid = new String[10][10];
        List<Ship> ships = new LinkedList<Ship>( Arrays.asList(new Ship("Carrier", 5), new Ship("Battleship", 4), new Ship("Cruiser", 3), new Ship("Submarine", 3), new Ship("Destroyer", 2)));

        while (ships.size()> 0)
        {
            Ship ship = ships.get(0);
            int positionX = 0 + (int)(Math.random() * 9);
            int positionY = 0 + (int)(Math.random() * 9);
            Boolean horizontal = (0 + (int)(Math.random() * 100)) % 2 == 1;

            if (grid[positionX][positionY] != null || !CheckShipCanFit(grid, ship.Size, positionX, positionY, horizontal)) continue;
            grid[positionX][positionY] = ship.Name;
            if (horizontal)
            {
                for (int i = 0; i < ship.Size; i++) grid[positionX+i][positionY] = ship.Name;
            }
            else
            {
                for (int i = 0; i < ship.Size; i++) grid[positionX][positionY + i] = ship.Name;
            }
            ships.remove(0);
        }
        return grid.toString();
    }

    private static Boolean CheckShipCanFit(String[][] grid, int shipSize, int positionX, int positionY, Boolean horizontal)
    {
        //check to see if there are enough spots to fit ships in
        Boolean cantFit = horizontal ? positionX + shipSize >= 10 : positionY + shipSize >= 10;
        if (cantFit) return false;

        //check to see if a shipB already placed in a spot that we need to occupy
        if (horizontal)
        {
            for (int i = positionX; i < shipSize; i++)
            {
                if (grid[i][positionY] != null) return false;
            }
        }
        else
        {
            for (int i = positionY; i < shipSize; i++)
            {
                if (grid[positionX][i] != null) return false;
            }
        }
        return true;
    }

    private static class Ship
    {
        public String Name;
        public int Size;

        /// <summary>Initializes a new instance of the <see cref="T:System.Object" /> class.</summary>
        public Ship(String name, int size)
        {
            Name = name;
            Size = size;
        }

        public String GetName()
        {
            return Name;
        }

        public int GetSize()
        {
            return Size;
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
                    e.printStackTrace();
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
