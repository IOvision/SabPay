# Place-Order
Body:
```json
{
    "orders": [
        {
            "items": [
                {item from firebase},
                {},
                {}...],
            "inventoryId": "inventoryId",
            "transactionId": "transactionId", // null if payment is not done 
            "discount": 50
        }
    ]
}
```
mehtod: POST<br>
urlDemo: https://us-central1-sabpay-ab94e.cloudfunctions.net/placeOrder?id={userId}&api_key={key}<br>
return: list of invoice object as json


# Generate Invoice
Body:
```json
{
    "orderId": "orderId",
    "transactionId": "transactoinId",
    "discount": 50
}
```
mehtod: POST<br>
urlDemo- https://us-central1-sabpay-ab94e.cloudfunctions.net/generateInvoice?api_key={key}<br>
return: list of invoice object as json

# Refund

mehtod: GET<br>
urlDemo- https://us-central1-sabpay-ab94e.cloudfunctions.net/refund?api_key={key}&userId={id}&transactionId={id}
