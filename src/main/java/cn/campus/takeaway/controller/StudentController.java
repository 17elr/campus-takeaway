package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.*;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.AddressService;
import cn.campus.takeaway.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {
    private final AppMapper mapper; private final OrderService orders; private final AddressService addresses;
    public StudentController(AppMapper mapper,OrderService orders,AddressService addresses){this.mapper=mapper;this.orders=orders;this.addresses=addresses;}
    @ModelAttribute("buildings") List<String> buildings(){return AddressService.BUILDINGS;}
    @GetMapping String dashboard(@AuthenticationPrincipal AppUser user,Model model){
        model.addAttribute("shops",mapper.listOpenShops()); model.addAttribute("addresses",mapper.listAddresses(user.getId()));
        var history=orders.studentOrders(user.getId()); model.addAttribute("orders",history);
        model.addAttribute("activeCount",history.stream().filter(order->!List.of("COMPLETED","CANCELED").contains(order.getStatus())).count());
        return "student";
    }
    @GetMapping("/shop/{id}") String shop(@PathVariable long id,Model model){
        Shop shop=mapper.findShop(id); available(shop); var dishes=mapper.listDishes(id).stream().filter(dish->Boolean.TRUE.equals(dish.getOnSale())).toList();
        model.addAttribute("shop",shop); model.addAttribute("dishes",dishes); model.addAttribute("categories",dishes.stream().map(Dish::getCategory).distinct().toList()); return "shop";
    }
    @PostMapping("/cart/add") String add(@RequestParam long dishId,@RequestParam(defaultValue="false") boolean stay,HttpSession session,RedirectAttributes flash){
        Dish dish=mapper.findDish(dishId); if(dish==null || !Boolean.TRUE.equals(dish.getOnSale())) throw new IllegalArgumentException("菜品不可购买");
        available(mapper.findShop(dish.getShopId())); cart(session).add(dish.getShopId(),dish.getId());
        flash.addFlashAttribute("success","已加入购物车"); return stay?"redirect:/student/shop/"+dish.getShopId():"redirect:/student/cart";
    }
    @PostMapping("/cart/remove") String remove(@RequestParam long dishId,HttpSession session){ cart(session).remove(dishId); return "redirect:/student/cart"; }
    @PostMapping("/cart/clear") String clear(HttpSession session,RedirectAttributes flash){cart(session).clear();flash.addFlashAttribute("success","购物车已清空");return "redirect:/student/cart";}
    @GetMapping("/cart") String cart(@AuthenticationPrincipal AppUser user,HttpSession session,Model model){
        Cart cart=cart(session);
        List<CartRow> rows;
        synchronized(cart){
            rows=cartRows(cart); model.addAttribute("cartRows",rows); model.addAttribute("checkoutToken",cart.getCheckoutToken());
        }
        var saved=mapper.listAddresses(user.getId()); model.addAttribute("addresses",saved);
        model.addAttribute("selectedAddressId",saved.isEmpty()?null:saved.get(0).getId());
        model.addAttribute("total",rows.stream().map(CartRow::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add)); return "cart";
    }
    @PostMapping("/orders") String create(@AuthenticationPrincipal AppUser user,@RequestParam long addressId,@RequestParam String checkoutToken,HttpSession session,RedirectAttributes flash){
        Cart cart=cart(session);
        // ponytail: one JVM session lock; persist checkout keys before adding multiple app instances.
        synchronized(cart){
            Long completed=cart.completedOrder(checkoutToken);
            if(completed!=null)return "redirect:/orders/"+completed;
            if(!cart.getCheckoutToken().equals(checkoutToken))throw new IllegalStateException("购物车已变化，请重新确认金额和地址后提交");
            Order order=orders.create(user.getId(),cart,addressId); cart.completeCheckout(order.getId());
            flash.addFlashAttribute("success","订单已创建，请完成模拟支付"); return "redirect:/orders/"+order.getId();
        }
    }
    @PostMapping("/orders/{id}/pay") String pay(@AuthenticationPrincipal AppUser user,@PathVariable long id,RedirectAttributes flash){ orders.pay(id,user.getId()); flash.addFlashAttribute("success","模拟支付成功，等待商家接单。未产生真实扣款。");return "redirect:/orders/"+id; }
    @PostMapping("/orders/{id}/cancel") String cancel(@AuthenticationPrincipal AppUser user,@PathVariable long id,RedirectAttributes flash){ orders.cancel(id,user.getId());flash.addFlashAttribute("success","订单已取消；模拟支付没有真实资金退款");return "redirect:/orders/"+id; }
    @PostMapping("/orders/{id}/confirm") String confirm(@AuthenticationPrincipal AppUser user,@PathVariable long id,RedirectAttributes flash){ orders.confirm(id,user.getId());flash.addFlashAttribute("success","已确认收货，订单完成");return "redirect:/orders/"+id; }
    @PostMapping("/orders/{id}/reorder") String reorder(@AuthenticationPrincipal AppUser user,@PathVariable long id,HttpSession session,RedirectAttributes flash){
        Cart replacement=orders.reorder(id,user.getId());
        synchronized(session){
            Cart current=cart(session);
            synchronized(current){
                current.clear();
                replacement.getQuantities().forEach((dishId,quantity)->{for(int index=0;index<quantity;index++)current.add(replacement.getShopId(),dishId);});
            }
        }
        flash.addFlashAttribute("success","已加入购物车，请核对现价和收货地址后下单");return "redirect:/student/cart";
    }
    @PostMapping("/addresses") String address(@AuthenticationPrincipal AppUser user,@RequestParam String building,@RequestParam String room,@RequestParam String contactName,@RequestParam String phone,RedirectAttributes flash){
        addresses.create(user.getId(),building,room,contactName,phone);flash.addFlashAttribute("success","校内地址已添加");return "redirect:/student?tab=addresses";
    }
    @PostMapping("/addresses/{id}/edit") String editAddress(@AuthenticationPrincipal AppUser user,@PathVariable long id,@RequestParam String building,@RequestParam String room,@RequestParam String contactName,@RequestParam String phone,RedirectAttributes flash){
        addresses.edit(user.getId(),id,building,room,contactName,phone); flash.addFlashAttribute("success","地址已更新，历史订单地址保持原快照");return "redirect:/student?tab=addresses";
    }
    @PostMapping("/addresses/{id}/delete") String deleteAddress(@AuthenticationPrincipal AppUser user,@PathVariable long id,RedirectAttributes flash){
        addresses.delete(user.getId(),id); flash.addFlashAttribute("success","地址已删除");return "redirect:/student?tab=addresses";
    }
    @PostMapping("/addresses/{id}/default") String defaultAddress(@AuthenticationPrincipal AppUser user,@PathVariable long id,RedirectAttributes flash){
        addresses.setDefault(user.getId(),id);flash.addFlashAttribute("success","默认地址已设置，结算时将自动选中");return "redirect:/student?tab=addresses";
    }
    private void available(Shop shop){if(shop==null || !Boolean.TRUE.equals(shop.getOpen()) || !Boolean.TRUE.equals(mapper.userEnabled(shop.getOwnerId())))throw new IllegalArgumentException("店铺休息中或已停用");}
    private Cart cart(HttpSession session){synchronized(session){Cart cart=(Cart)session.getAttribute("cart");if(cart==null){cart=new Cart();session.setAttribute("cart",cart);}return cart;}}
    private List<CartRow> cartRows(Cart cart){var rows=new ArrayList<CartRow>();cart.getQuantities().forEach((id,quantity)->{Dish dish=mapper.findDish(id);if(dish!=null)rows.add(new CartRow(dish,quantity,dish.getPrice().multiply(BigDecimal.valueOf(quantity))));});return rows;}
}
