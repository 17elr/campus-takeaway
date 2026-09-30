package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.AppUser;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.InputChecks;
import cn.campus.takeaway.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;

@Controller @RequestMapping("/merchant")
public class MerchantController {
    private final AppMapper mapper; private final OrderService orders;
    public MerchantController(AppMapper mapper,OrderService orders){this.mapper=mapper;this.orders=orders;}
    @GetMapping String dashboard(@AuthenticationPrincipal AppUser user,Model model){
        var shop=mapper.findShopByOwner(user.getId()); if(shop==null)throw new IllegalStateException("店铺信息不存在，请联系管理员");
        var history=orders.shopOrders(shop.getId());model.addAttribute("shop",shop);model.addAttribute("dishes",mapper.listDishes(shop.getId()));model.addAttribute("orders",history);
        model.addAttribute("pendingCount",history.stream().filter(order->"PENDING_ACCEPT".equals(order.getStatus())).count());
        model.addAttribute("makingCount",history.stream().filter(order->"MAKING".equals(order.getStatus())).count());
        model.addAttribute("completedAmount",history.stream().filter(order->"COMPLETED".equals(order.getStatus())).map(order->order.getAmount()).reduce(BigDecimal.ZERO,BigDecimal::add)); return "merchant";
    }
    @PostMapping("/orders/{id}/accept") String accept(@AuthenticationPrincipal AppUser user,@PathVariable long id,RedirectAttributes flash){orders.merchantAccept(id,user.getId());flash.addFlashAttribute("success","已接单，请开始制作");return "redirect:/merchant?tab=orders";}
    @PostMapping("/orders/{id}/ready") String ready(@AuthenticationPrincipal AppUser user,@PathVariable long id,RedirectAttributes flash){orders.merchantReady(id,user.getId());flash.addFlashAttribute("success","制作完成，订单已进入待配送任务");return "redirect:/merchant?tab=orders";}
    @PostMapping("/dishes") String dish(@AuthenticationPrincipal AppUser user,@RequestParam String category,@RequestParam String name,@RequestParam BigDecimal price,RedirectAttributes flash){
        category=InputChecks.text(category,"分类",40);name=InputChecks.text(name,"名称",80);price=InputChecks.price(price);
        mapper.insertDish(mapper.findShopByOwner(user.getId()).getId(),category,name,price);flash.addFlashAttribute("success","菜品已添加并上架");return "redirect:/merchant?tab=dishes";
    }
    @PostMapping("/dishes/{id}/edit") String edit(@AuthenticationPrincipal AppUser user,@PathVariable long id,@RequestParam String category,@RequestParam String name,@RequestParam BigDecimal price,RedirectAttributes flash){
        category=InputChecks.text(category,"分类",40);name=InputChecks.text(name,"名称",80);price=InputChecks.price(price);
        if(mapper.updateDish(id,mapper.findShopByOwner(user.getId()).getId(),name,category,price)!=1)throw new IllegalArgumentException("菜品不存在或无权修改");flash.addFlashAttribute("success","菜品已更新，历史订单单价保持原快照");return "redirect:/merchant?tab=dishes";
    }
    @PostMapping("/dishes/{id}/toggle") String toggle(@AuthenticationPrincipal AppUser user,@PathVariable long id,RedirectAttributes flash){if(mapper.toggleDish(id,mapper.findShopByOwner(user.getId()).getId())!=1)throw new IllegalArgumentException("菜品不存在或无权修改");flash.addFlashAttribute("success","菜品上下架状态已更新");return "redirect:/merchant?tab=dishes";}
    @PostMapping("/shop") String shop(@AuthenticationPrincipal AppUser user,@RequestParam String name,@RequestParam String description,@RequestParam boolean open,RedirectAttributes flash){
        name=InputChecks.text(name,"店名",80);description=InputChecks.text(description,"店铺简介",255);mapper.updateShop(user.getId(),name,description,open);flash.addFlashAttribute("success","店铺信息已保存");return "redirect:/merchant?tab=shop";
    }
}
