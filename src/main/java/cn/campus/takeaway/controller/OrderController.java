package cn.campus.takeaway.controller;

import cn.campus.takeaway.domain.*;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.OrderService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;
    private final AppMapper mapper;
    public OrderController(OrderService orders,AppMapper mapper) { this.orders=orders; this.mapper=mapper; }
    @GetMapping
    PageResult<Order> list(@RequestAttribute("currentUser") AppUser user,@RequestParam(defaultValue="1") int page,
        @RequestParam(defaultValue="12") int size,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="mine") String scope,@RequestParam(defaultValue="") String query) {
        return orders.page(user,page,size,status,scope,query);
    }
    @GetMapping("/{id}")
    Map<String,Object> detail(@RequestAttribute("currentUser") AppUser user,@PathVariable long id) {
        var order=orders.authorizedOrder(id,user);
        return Map.of("order",order,"events",mapper.listEvents(id),"paymentMode","SIMULATED");
    }
    @PostMapping("/{id}/{action:pay|cancel|confirm|accept|ready|claim|delivered}")
    Map<String,String> action(@RequestAttribute("currentUser") AppUser user,@PathVariable long id,@PathVariable String action) {
        String role=switch(action) { case "pay","cancel","confirm" -> "STUDENT"; case "accept","ready" -> "MERCHANT"; default -> "RIDER"; };
        if (!role.equals(user.getRole())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"当前角色无权执行此动作");
        switch(action) {
            case "pay" -> orders.pay(id,user.getId());
            case "cancel" -> orders.cancel(id,user.getId());
            case "confirm" -> orders.confirm(id,user.getId());
            case "accept" -> orders.merchantAccept(id,user.getId());
            case "ready" -> orders.merchantReady(id,user.getId());
            case "claim" -> orders.riderClaim(id,user.getId());
            case "delivered" -> orders.riderDelivered(id,user.getId());
        }
        return Map.of("message","订单已更新");
    }
}
