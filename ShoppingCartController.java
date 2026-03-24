# Product service definition
class Product:
    product_id: int
    sku: str
    manufacturer: str
    category_id: int
    weight: int
    some_other_id: int

def get( productID: int ) -> Product:
    pass
def add( product_id: int, details: Product ) -> void:
    pass
# Shopping Cart Service Definition
def create( customer_id: int) -> int:
    pass
def add_items( shopping_cart_id: int, product_id: int, quantity: int ) -> void:
    pass
def checkout( shopping_cart_id: int) -> int:
    pass
# Warehouse Service Definition
def reserve( product_ID: int, quantity: int) -> void
    pass
def ship( product_ID: int, quantity: int)  -> void
    pass

# Creditcard service definition
def checkout( credit_card_number: str, shopping_cart_id: int ) -> bool:
    pass
