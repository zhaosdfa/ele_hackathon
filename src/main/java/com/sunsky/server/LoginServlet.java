package com.sunsky.server;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import org.json.JSONObject;
import org.json.JSONArray;


public class LoginServlet extends HttpServlet {

    public void service(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        try {

            res.setCharacterEncoding("utf-8");

            ResponseResult result = _handle(req, res);
            String responseMsg = result.getBody();

            //Utils.println("LoginServlet: code: " + result.getCode() + "; msg: " + responseMsg);

            res.setStatus(result.getCode());
            res.getWriter().println(responseMsg);

        } catch (Exception e) {
            Utils.print(e);
        }
    }

    public ResponseResult _handle(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {

        String requestMethod = req.getMethod();

        int bodyCode = 200;

        String data = Utils.readBody(req);

        if (data == null) {
            JSONObject obj = new JSONObject();
            obj.put("code", "EMPTY_REQUEST");
            obj.put("message", "请求体为空");
            return new ResponseResult(400, obj.toString());
        }

        JSONObject user = null;
        String username = null;
        String password = null;
        try {
            user = new JSONObject(data);
            username = user.getString("username");
            password = user.getString("password");
            if (username == null || password == null) throw new Exception();
        } catch (Exception e) {
            JSONObject obj = new JSONObject();
            obj.put("code", "MALFORMED_JSON");
            obj.put("message", "格式错误");
            return new ResponseResult(400, obj.toString());
        }

        int code = 403;
        JSONObject body = new JSONObject();
        User tuser = null;
        if ((tuser = UserDAO.get(username)) == null || !tuser.getPassword().equals(password)) {
            body.put("code", "USER_AUTH_FAIL");
            body.put("message", "用户名或密码错误");
            code = 403;
        } else {
            body.put("user_id", tuser.getId());
            body.put("username", username);
            body.put("access_token", tuser.getAccessToken());
            code = 200;
        }

        return new ResponseResult(code, body.toString());
    }

}
