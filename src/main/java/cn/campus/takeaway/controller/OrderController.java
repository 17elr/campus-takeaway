package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.AppUser;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class OrderController {
    private final OrderService orders; private final AppMapper mapper;
    public OrderController(OrderService orders,AppMapper mapper){this.orders=orders;this.mapper=mapper;}
    @GetMapping("/orders/{id}") String detail(@PathVariable long id,@AuthenticationPrincipal AppUser user,Model model) {
        model.addAttribute("order",orders.authorizedOrder(id,user)); model.addAttribute("events",mapper.listEvents(id)); return "order-detail";
    }
}
