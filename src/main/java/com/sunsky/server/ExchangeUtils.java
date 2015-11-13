package com.sunsky.server;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import java.lang.Math;

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

	public static String getHeader(HttpExchange exchange, String key) {
		Headers headers = exchange.getRequestHeaders();
		List<String> tmp = headers.get(key);
		if (tmp != null && tmp.size() > 0) {
			return tmp.get(0);
		}
		return null;
	}

	public static String getRequestBody(HttpExchange exchange, String encode) {
		Headers headers = exchange.getRequestHeaders();
		List<String> lens = headers.get("Content-Length");
		int length = 0; // body length
		if (lens != null && lens.size() > 0) {
			String tmp = lens.get(0);
			try {
				length = Integer.parseInt(tmp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("len: " + length);
		if (length <= 0) {
			return null;
		}
		InputStream in = exchange.getRequestBody();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[BUFFER_SIZE];
		int count = 0;
		int sum = 0;
		String result = null;
		try {

			while (sum < length && (count = in.read(buf, 0, Math.min(BUFFER_SIZE, length-sum))) != -1) {
				out.write(buf, 0, count);
				sum += count;
			}
			if (sum != length) throw new IOException();
			result = new String(out.toByteArray(), encode);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

}
