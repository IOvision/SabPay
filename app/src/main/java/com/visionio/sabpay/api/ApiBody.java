package com.visionio.sabpay.api;

import com.visionio.sabpay.models.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiBody {

    /*
    {
        orders: [
            {
            items: [],
            inventoryId: inventoryId,
            transactionId: transactionId, // null if payment is not done
            discount: 50
            }
        ]
    }
     */

    public static Map<String, Object> buildPlaceOrderBody(List<Item> items, String inventoryId, String transactionId,
                                                          float discount){
        Map<String, Object> body = new HashMap<>();

        List<Map<String, Object>> orderList = new ArrayList<>();
        List<Map<String, Object>> itemsList = new ArrayList<>();

        Map<String, Object> item = new HashMap<>();

        for(Item i: items){
            Map<String, Object> iMap = new HashMap<>();
            iMap.put("id", i.getId());
            iMap.put("inventoryId", i.getInventory_id());
            iMap.put("title", i.getTitle());
            iMap.put("description", i.getDescription());
            iMap.put("unit", i.getUnit());
            iMap.put("cost", i.getCost());
            iMap.put("qty", i.getQty());
            itemsList.add(iMap);
        }

        item.put("inventoryId", inventoryId);
        item.put("transactionId", transactionId);
        item.put("discount", discount);
        item.put("items", itemsList);

        orderList.add(item);

        body.put("orders", orderList);


        return body;
    }


}
