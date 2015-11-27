package com.sunsky.server;

import java.io.*;
import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static final PrintStream out = System.out;

    public static final boolean DEBUG = false;

    public static void println(String msg) {
	if (DEBUG) {
	    out.println(msg);
	}
    }

    public static void print(Exception e) {
	if (DEBUG) {
	    e.printStackTrace(out);
	}
    }


    public static final int BUFFER_SIZE = 64;
    public static final String ENCODE = "utf-8";

    public static String read(InputStream in) throws Exception {  
	ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	byte[] data = new byte[BUFFER_SIZE];
	int count = -1;
	while((count = in.read(data,0,BUFFER_SIZE)) != -1) {
	    outStream.write(data, 0, count);
	}

	data = null;
	String ret = new String(outStream.toByteArray(), ENCODE); 
	if (ret.length() == 0) {
	    return null;
	}
	return ret;
    } 

    public static String readBody(HttpServletRequest req) {
	String body = null;
	try {
	    body = read(req.getInputStream());
	} catch (Exception e) {
	    print(e);
	}
	return body;
    }

    public static String MD5(String msg) {
	char hexDigits[]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};       
	try {
	    byte[] btInput = msg.getBytes();
	    MessageDigest mdInst = MessageDigest.getInstance("MD5");
	    mdInst.update(btInput);
	    byte[] md = mdInst.digest();
	    int j = md.length;
	    char str[] = new char[j * 2];
	    int k = 0;
	    for (int i = 0; i < j; i++) {
		byte byte0 = md[i];
		str[k++] = hexDigits[byte0 >>> 4 & 0xf];
		str[k++] = hexDigits[byte0 & 0xf];
	    }
	    return new String(str);
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }

}
