
## Use Cases

### Customer Adds Item to Shopping Cart
**Prerequisites**
1. Customer is logged in, therefore their browser has their customer ID. 

**Steps**
1. Customer selects a product
2. Customer chooses quantity
3. Customer clicks the "Add to Cart Button"
4. System adds the item to the active Shopping cart

**Errors and Exceptions**
1. At step 3a, If there is no active shopping cart then the System creates one adn returns its ID to the client.
2. Standard bounds checking on quantity and product ID (ie, quantity in range 1..10_000 , valid ProductID)

### Customer Checks-Out Shopping Cart
1. Customer is logged in, so their browser has their customer ID.

**Steps**
1. Customer adds Credit Card Information
2. Customer clicks "Checkout"
3. Credit Card Company Authorizes the charge
4. System contacts Warehouse to tell it to prepare the shipment

**Errors and Exceptions**
1. Credit Card Company Declines the charge, return error to client and stop