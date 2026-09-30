package cn.campus.takeaway.domain;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Cart implements Serializable {
    private Long shopId;
    private final Map<Long,Integer> quantities=new LinkedHashMap<>();
    private String checkoutToken=UUID.randomUUID().toString();
    private String submittedToken;
    private Long submittedOrderId;
    public synchronized Long getShopId(){return shopId;}
    public synchronized Map<Long,Integer> getQuantities(){return new LinkedHashMap<>(quantities);}
    public synchronized String getCheckoutToken(){return checkoutToken;}
    public synchronized Long completedOrder(String token){return token!=null && token.equals(submittedToken)?submittedOrderId:null;}
    public synchronized void completeCheckout(long orderId){
        submittedToken=checkoutToken; submittedOrderId=orderId; clear();
    }
    public synchronized void add(Long shop,Long dish){
        if(shopId!=null && !shopId.equals(shop) && !quantities.isEmpty()) throw new IllegalArgumentException("一次只能选择一家商家的菜品，请先清空购物车");
        if(quantities.getOrDefault(dish,0)>=99) throw new IllegalArgumentException("每道菜最多 99 份");
        shopId=shop; quantities.merge(dish,1,Integer::sum); changed();
    }
    public synchronized void remove(Long dish){
        if(!quantities.containsKey(dish))return;
        quantities.computeIfPresent(dish,(k,v)->v>1?v-1:null); if(quantities.isEmpty())shopId=null; changed();
    }
    public synchronized void clear(){quantities.clear(); shopId=null; changed();}
    private void changed(){checkoutToken=UUID.randomUUID().toString();}
}
