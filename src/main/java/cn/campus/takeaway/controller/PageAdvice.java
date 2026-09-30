package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.AppUser;
import cn.campus.takeaway.domain.Cart;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class PageAdvice {
    @ModelAttribute
    public void common(@AuthenticationPrincipal AppUser user, HttpServletRequest request, Model model) {
        model.addAttribute("currentUser",user);
        model.addAttribute("roleLabel",user==null?"":switch(user.getRole()){case "STUDENT"->"学生中心";case "MERCHANT"->"商家工作台";case "RIDER"->"骑手工作台";default->"平台管理";});
        model.addAttribute("roleHome",user==null?"/":("/"+user.getRole().toLowerCase()));
        model.addAttribute("currentPath",request.getRequestURI());
        String tab=request.getParameter("tab");
        if(tab==null)tab=request.getRequestURI().startsWith("/orders/")?"orders":"overview";
        model.addAttribute("tab",tab);
        var session=request.getSession(false); Cart cart=session==null?null:(Cart)session.getAttribute("cart");
        model.addAttribute("cartCount",cart==null?0:cart.getQuantities().values().stream().mapToInt(Integer::intValue).sum());
    }
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class,DataIntegrityViolationException.class})
    public String failure(Exception exception, @AuthenticationPrincipal AppUser user, HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("error",exception instanceof DataIntegrityViolationException?"数据冲突，请检查用户名或刷新后重试":exception.getMessage());
        if(user==null) return "redirect:/register";
        String home="/"+user.getRole().toLowerCase();
        String path=request.getRequestURI();
        if(path.startsWith("/account"))return "redirect:/account";
        if("STUDENT".equals(user.getRole()) && path.equals("/student/orders"))return "redirect:/student/cart";
        String tab=path.contains("addresses")?"addresses":path.contains("dishes")?"dishes":path.contains("orders")?"orders":path.contains("shops")?"shops":path.contains("users")?"users":"overview";
        return "redirect:"+home+(path.contains("cart")&&"STUDENT".equals(user.getRole())?"/cart":"?tab="+tab);
    }
    @ExceptionHandler({AccessDeniedException.class,ResponseStatusException.class,ServletRequestBindingException.class,MethodArgumentTypeMismatchException.class,NoResourceFoundException.class})
    public ModelAndView error(Exception exception){
        int status=exception instanceof AccessDeniedException?403:exception instanceof ResponseStatusException response?response.getStatusCode().value():exception instanceof NoResourceFoundException?404:400;
        var view=new ModelAndView("error"); view.setStatus(HttpStatus.valueOf(status)); view.addObject("status",status); return view;
    }
}
