
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;

public class ExchangeUtils {

    public static final int BUFFER_SIZE = 1024;

    public static final String ENCODE = "utf-8";

    /**
     * read parameters from request URI(GET) and request body(POST)
     */
	public static Map<String, String> getParameters(HttpExchange exchange) {
		Map<String, String> result = new HashMap<String, String>();

		// GET parameters
		String uri = exchange.getRequestURI().toString();

		// /login?user=aaa&pwd=bbb
		String[] part = uri.split("\\?");

		if (part.length == 2) {
			String[] parameters = part[1].split("&");
			for (String para : parameters) {
				String[] tmp = para.split("=");
				if (tmp.length == 2) {
					result.put(tmp[0], tmp[1]);
				}
			}
		}

		// POST parameters
		try {
			InputStream in = exchange.getRequestBody();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[BUFFER_SIZE];
			int count = 0;
			while ((count = in.read(buf, 0, BUFFER_SIZE)) != -1) {
				out.write(buf, 0, count);
			}

			String data = new String(out.toByteArray(), ENCODE);

			String[] parameters = data.split("&");
			for (String para : parameters) {
				String[] tmp = para.split("=");
				if (tmp.length == 2) {
					result.put(tmp[0], tmp[1]);
				}
			}
		} catch (IOException e) {
			System.out.println(e.toString());
		}

		return result;
	}

    public static Map<String, String> getGetParameters(HttpExchange exchange) {
		Map<String, String> result = new HashMap<String, String>();
        String uri = exchange.getRequestURI().toString();
		String[] part = uri.split("\\?");

		if (part.length == 2) {
			String[] parameters = part[1].split("&");
			for (String para : parameters) {
				String[] tmp = para.split("=");
				if (tmp.length == 2) {
					result.put(tmp[0], tmp[1]);
				}
			}
		}

        return result;
    }

    public static String getRequestBody(HttpExchange exchange, String encode) throws IOException {
        /*
        Headers headers = exchange.getRequestHeaders();
        List<String> lens = headers.get("Content-Length");
        int length = 0; // body length
        if (lens.size() > 0) {
            String tmp = lens.get(0);
            try {
                length = Integer.parseInt(tmp);
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
        System.out.println("len: " + length);
        */
        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[BUFFER_SIZE];
        int count = 0;
        while ((count = in.read(buf, 0, BUFFER_SIZE)) != -1) {
            out.write(buf, 0, count);
        }
        return new String(out.toByteArray(), encode);
    }

}
