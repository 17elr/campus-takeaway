package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.AppUser;
import cn.campus.takeaway.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller @RequestMapping("/rider")
public class RiderController {
    private final OrderService orders;
    public RiderController(OrderService orders){this.orders=orders;}
    @GetMapping String dashboard(@AuthenticationPrincipal AppUser user,Model model){var history=orders.riderOrders(user.getId());model.addAttribute("orders",history);model.addAttribute("availableOrders",history.stream().filter(order->"READY".equals(order.getStatus())&&order.getRiderId()==null).toList());model.addAttribute("myOrders",history.stream().filter(order->java.util.Objects.equals(order.getRiderId(),user.getId())).toList());model.addAttribute("currentRiderId",user.getId());return "rider";}
    @PostMapping("/orders/{id}/claim") String claim(@AuthenticationPrincipal AppUser user,@PathVariable long id,RedirectAttributes flash){orders.riderClaim(id,user.getId());flash.addFlashAttribute("success","接单成功，已进入配送中");return "redirect:/rider?tab=orders";}
    @PostMapping("/orders/{id}/delivered") String delivered(@AuthenticationPrincipal AppUser user,@PathVariable long id,RedirectAttributes flash){orders.riderDelivered(id,user.getId());flash.addFlashAttribute("success","已标记送达，等待学生确认");return "redirect:/rider?tab=orders";}
}
