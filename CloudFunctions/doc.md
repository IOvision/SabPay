# Place-Order
{
    "orders": [
        {
            "items": [
                {
                    "id": "1Spe5y6MNL4jYdV8Q7Ok",
                    "cost": 14,
                    "qty": 2,
                    "title": "Bislarei",
                    "description": "badiya pani"
                }
            ],
            "inventoryId": "uWcOQvpGl3nwyhWviGgE",
            "transactionId": null,
            "discount": 50
        }
    ]
}


urlDemo- https://us-central1-sabpay-ab94e.cloudfunctions.net/placeOrder?id={userId}&api_key={key}


# Generate Invoice

{
    "orderId": "qxrZaWIP1cIUCK4qsd3s",
    "transactionId": "G9KKMPHXX9NCIhwHGK4C",
    "discount": 50
}

urlDemo- https://us-central1-sabpay-ab94e.cloudfunctions.net/refund?api_key={key}&userId={id}&transactionId={id}
