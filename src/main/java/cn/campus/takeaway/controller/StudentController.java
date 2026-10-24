package cn.campus.takeaway.controller;

import cn.campus.takeaway.cart.CartService;
import cn.campus.takeaway.domain.*;
import cn.campus.takeaway.mapper.AppMapper;
import cn.campus.takeaway.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    private final AppMapper mapper;
    private final CartService carts;
    private final OrderService orders;
    private final AddressService addresses;
    public StudentController(AppMapper mapper,CartService carts,OrderService orders,AddressService addresses) {
        this.mapper=mapper; this.carts=carts; this.orders=orders; this.addresses=addresses;
    }
    public record Quantity(@Min(1) @Max(99) int quantity) {}
    public record Checkout(@NotBlank String checkoutKey,@NotNull @Min(0) Long cartVersion,@NotNull @Min(1) Long addressId) {}
    public record AddressInput(@NotBlank String building,@NotBlank String room,@NotBlank String contactName,@NotBlank String phone) {}
    @GetMapping("/cart")
    CartService.View cart(@RequestAttribute("currentUser") AppUser user) { return carts.get(user.getId()); }
    @PutMapping("/cart/items/{id}")
    CartService.View put(@RequestAttribute("currentUser") AppUser user,@PathVariable long id,@Valid @RequestBody Quantity input) { return carts.put(user.getId(),id,input.quantity()); }
    @DeleteMapping("/cart/items/{id}")
    CartService.View remove(@RequestAttribute("currentUser") AppUser user,@PathVariable long id) { return carts.remove(user.getId(),id); }
    @DeleteMapping("/cart")
    CartService.View clear(@RequestAttribute("currentUser") AppUser user) { return carts.clear(user.getId()); }
    @PostMapping("/orders")
    Order checkout(@RequestAttribute("currentUser") AppUser user,@Valid @RequestBody Checkout input) {
        return orders.create(user.getId(),input.checkoutKey(),input.cartVersion(),input.addressId());
    }
    @PostMapping("/orders/{id}/reorder")
    CartService.View reorder(@RequestAttribute("currentUser") AppUser user,@PathVariable long id) { return orders.reorder(id,user.getId()); }
    @GetMapping("/addresses")
    Map<String,Object> addresses(@RequestAttribute("currentUser") AppUser user) { return Map.of("records",mapper.listAddresses(user.getId()),"buildings",AddressService.BUILDINGS); }
    @PostMapping("/addresses")
    Map<String,String> add(@RequestAttribute("currentUser") AppUser user,@Valid @RequestBody AddressInput input) {
        addresses.create(user.getId(),input.building(),input.room(),input.contactName(),input.phone()); return ok();
    }
    @PutMapping("/addresses/{id}")
    Map<String,String> edit(@RequestAttribute("currentUser") AppUser user,@PathVariable long id,@Valid @RequestBody AddressInput input) {
        addresses.edit(user.getId(),id,input.building(),input.room(),input.contactName(),input.phone()); return ok();
    }
    @DeleteMapping("/addresses/{id}")
    Map<String,String> delete(@RequestAttribute("currentUser") AppUser user,@PathVariable long id) { addresses.delete(user.getId(),id); return ok(); }
    @PostMapping("/addresses/{id}/default")
    Map<String,String> defaultAddress(@RequestAttribute("currentUser") AppUser user,@PathVariable long id) { addresses.setDefault(user.getId(),id); return ok(); }
    private Map<String,String> ok() { return Map.of("message","地址已更新"); }
}
