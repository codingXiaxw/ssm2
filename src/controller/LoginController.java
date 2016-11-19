package controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

/**
 * Created by codingBoy on 16/11/18.
 * 完成登陆和退出
 */
@Controller
public class LoginController
{

    //用户登陆提交方法
    @RequestMapping("/login")
    public String login(HttpSession session,String usercode, String password) throws Exception
    {
        //调用service校验用户帐号和密码的正确性
        //这个东西我们讲shiro的时候再写



        //如果service校验通过，将用户身份记录到session
        session.setAttribute("usercode",usercode);

        //重定向到商品查询页面
        return "redirect:/items/queryItems.action";
    }

    //用户退出
    @RequestMapping("/logout")
    public String logout(HttpSession session) throws Exception
    {
        //session失效
        session.invalidate();

        //重定向到商品查询页面
        return "redirect:/items/queryItems.action";
    }
}
