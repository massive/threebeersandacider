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
import java.util.stream.Collector;
import java.util.stream.Collectors;

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
        List<Ship> ships = new LinkedList<Ship>( Arrays.asList(new Ship(1,"Carrier", 5), new Ship(2,"Battleship", 4), new Ship(3,"Cruiser", 3), new Ship(4,"Submarine", 3), new Ship(5,"Destroyer", 2)));

        while (ships.size()> 0)
        {
            Ship ship = ships.get(0);
            int positionX = 0 + (int)(Math.random() * 9);
            int positionY = 0 + (int)(Math.random() * 9);
            Boolean horizontal = (0 + (int)(Math.random() * 100)) % 2 == 1;

            if (grid[positionX][positionY] != null || !CheckShipCanFit(grid, ship.Size, positionX, positionY, horizontal)) continue;
            grid[positionX][positionY] = String.valueOf(ship.ID);
            if (horizontal)
            {
                for (int i = 0; i < ship.Size; i++) grid[positionX+i][positionY] = String.valueOf(ship.ID);
            }
            else
            {
                for (int i = 0; i < ship.Size; i++) grid[positionX][positionY + i] = String.valueOf(ship.ID);
            }
            ships.remove(0);
        }

        //remove nulls
        for (int i=0; i < 10; i++)
            for (int j=0; j<10; j++)
                if (grid[i][j] == null)
                    grid[i][j] = " ";

        return "{ grid: ["+ Arrays.stream(grid).map(strings -> {
            return "'" + Arrays.stream(strings).collect(Collectors.joining("', '")) + "'";
        }).collect(Collectors.joining("], \n [")) + "]}";
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
        public int ID;
        public String Name;
        public int Size;

        /// <summary>Initializes a new instance of the <see cref="T:System.Object" /> class.</summary>
        public Ship(int id, String name, int size)
        {
            ID = id;
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
            String event = "" + ((JSONObject)((JSONObject)params.get("report")).get("you")).get("event");
            System.out.println("Got event: " + event);
            State setLastTargetTo = State.SHOT;
            switch (event) {
                case "SUNK":
                    // TODO: Set the rest of the ship to SUNK.
                    setLastTargetTo = State.SUNK;
                    break;
                case "HIT":
                    setLastTargetTo = State.SHIP;
                    break;
                case "MISS":
                    setLastTargetTo = State.NO_SHIP;
                    break;
                default:
                    System.err.println("Invalid event: " + event);
                    break;
            }
            if (lastTarget != null) setState(lastTarget, setLastTargetTo);

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
        UNKNOWN, SHOT, SHIP, SUNK, NO_SHIP, INVALID
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

    private static int[] lastTarget = null;

    private static int[] calculateTarget() {
        int[] target = findTargetNextToShip();
        if (target == null) {
            while (true) {
                target = getRandomTarget();
                if (getState(target) == State.UNKNOWN) break;
            }
        }
        System.out.println("Target: " + target[0] + ", " + target[1]);
        setState(target, State.SHOT);
        lastTarget = target;
        return target;
    }

    private static int[] findShip() {
        for (int y = 0; y < SIZE_Y; y++) {
            for (int x = 0; x < SIZE_X; x++) {
                int target[] = new int[]{x, y};
                if (getState(target) == State.SHIP) {
                    System.out.println("Ship: " + target[0] + ", " + target[1]);
                    return target;
                }
            }
        }
        return null;
    }

    private static int[] move(int[] pos, int xd, int yd) {
        return new int[]{pos[0] + xd, pos[1] + yd};
    }

    private static int[] findTargetNextToShip() {
        int ship[] = findShip();
        if (ship == null) return null;
        if (getState(move(ship, -1, 0)) == State.UNKNOWN) return move(ship, -1, 0);
        if (getState(move(ship, 1, 0)) == State.UNKNOWN) return move(ship, 1, 0);
        if (getState(move(ship, 0, -1)) == State.UNKNOWN) return move(ship, 0, -1);
        if (getState(move(ship, 0, 1)) == State.UNKNOWN) return move(ship, 0, 1);
        return null;
    }

    private static State getState(int[] target) {
        final int x = target[0];
        final int y = target[1];
        if (x < 0 || y < 0 || x >= SIZE_X || y >= SIZE_Y) return State.INVALID;
        return grid[x][y];
    }

    private static void setState(int[] target, State setTo) {
        final int x = target[0];
        final int y = target[1];
        System.out.println("Setting " + x + ", " + y + " to " + setTo);
        grid[x][y] = setTo;
    }

    private static int[] getRandomTarget() {
        return new int[]{Math.abs(random.nextInt()) % SIZE_X, Math.abs(random.nextInt()) % SIZE_Y};
    }
}
