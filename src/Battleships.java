//
// Compiling:
// javac -classpath .:lib/json-simple-1.1.1.jar src/Battleships.java
//
// Starting the server:
// java -classpath .:lib/json-simple-1.1.1.jar src/Battleships
//

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import jdk.nashorn.internal.ir.debug.JSONWriter;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.JSONParser;

public class Battleships {

    public static void main(String[] args) throws Exception {
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
            String response = getResponse();
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        protected abstract String getResponse();
    }

    static class StartGameHandler extends HandlerWithStringResponse {
        @Override
        protected String getResponse()
        {
            return getNewGrid();
        }
    }

    private static String getNewGrid()
    {
        String[][] grid = new String[10][10];
        List<Ship> ships = Arrays.asList(new Ship("Carrier", 5), new Ship("Battleship", 4), new Ship("Cruiser", 3), new Ship("Submarine", 3), new Ship("Destroyer", 2));

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
        protected String getResponse() {
            return "{\n" +
                "  x: 6,\n" +
                "  y: 2\n" +
                "}\n";
        }
    }

    static class EndGameHandler extends HandlerWithStringResponse {
        @Override
        protected String getResponse() {
            return "{\n" +
                "  \"message\" : \"AAAGH!\"\n" +
                "}\n";
        }
    }
}