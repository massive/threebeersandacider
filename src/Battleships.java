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

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Battleships {

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        System.out.println("Running at " + server.getAddress());

        HttpContext ctx1 = server.createContext("/start_game", new StartGameHandler());

        HttpContext ctx2 = server.createContext("/next_turn", new NextTurnHandler());

        server.createContext("/end_game", new EndGameHandler());
        server.start();
    }

    static abstract class HandlerWithStringResponse implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            InputStream bodyStream = t.getRequestBody();
            String requestBody = convertStreamToString(bodyStream);
            Object parsed;

            try {
                parsed = new JSONParser().parse(requestBody);
            } catch (ParseException e) {
                e.printStackTrace();
                return;
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
            return "{\n" +
                "  x: 6,\n" +
                "  y: 2\n" +
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
}
