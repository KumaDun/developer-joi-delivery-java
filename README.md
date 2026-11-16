# Welcome to JOI Delivery
JOI Delivery is built for real life. For the young professional who gets home late and doesn’t have the energy to cook. For the student with an exam tomorrow and an empty fridge tonight. These aren’t exceptions — they’re everyday moments. That’s why JOI Delivery brings food and groceries to your door, fast, fresh, and right when you need them.

Customers struggle with:

- Cluttered browsing experiences that don’t understand their preferences.
- Limited customization when ordering meals or groceries.
- Unclear order status or delivery timelines.
- Poor payment experience, or failed checkouts.
- Lack of timely feedback channels to report a bad experience or appreciate a good one.

JOI Delivery was built not just as another delivery app, but as a thoughtful, technology-first platform that reimagines how essentials reach customers in the most seamless way.

# Introducing JOI Delivery

JOI Delivery, launched in 2024, is a hyperlocal delivery app designed to bring food and groceries to your doorstep in under 45 minutes. With the tagline "Speed meets convenience," it connects customers to nearby restaurants and stores through a seamless digital experience. The app solves the hassle of long wait times and limited local options by offering real-time tracking, instant order updates, and a wide network of trusted vendors.

## Business Goals
- Differentiated Value Proposition & Niche Dominance
- Deliver Unmatched Customer Experience & Loyalty
- Superior Operational Efficiency & Cost Advantage
- Robust & Engaged Partner Ecosystem

## Why they need Thoughtworks help
As JOI Delivery continues to grow and serve more neighborhoods, we’re scaling our platform to handle increasing demand, enhance user experience, and support smarter delivery logistics. They're looking for passionate developers to help us build robust, efficient, and scalable solutions that power everything from order placement to real-time tracking.
Your expertise will directly impact how quickly and reliably customers receive their essentials—and how smoothly local vendors and delivery partners operate within our ecosystem.

### Users/Customers
Sample user profiles are available in the repository to support development and testing scenarios.

| UserId   | FirstName | LastName|
|----------|-----------|---------|
| user101  | John      | Doe     |

### Stores
Sample store data seeded for development purposes only.

| StoreId  | OutletName     |
|----------|----------------|
| store101 | Fresh Picks    |
| store102 | Natural Choice |

### Grocery Products
Dummy Products for Stores to sell and users to buy from.

| ProductId  | ProductName | StoreRefId |
|------------|-------------|------------|
| product101 | Wheat Bread | store101   |
| product102 | Spinach     | store101   |
| product103 | Crackers    | store101   |

### Restaurant Meals

The seeded restaurant `restaurant101` (Spice Kitchen) provides prepared meals.
Meal prices and availability are stored on `FoodProduct`, and each meal belongs
to its restaurant's menu. Grocery stores and restaurants share the outlet repository.

Register catalog products through the outlet's `addProduct` method. Products sold
by different outlets use separate instances and distinct product IDs. Registration
rejects an existing owner from another outlet and duplicate IDs within the same
outlet. Menus and grocery inventories are exposed as read-only snapshots.

| ProductId | ProductName | OutletId | Unit Price | Available Stock |
|-----------|-------------|----------|------------|-----------------|
| meal101 | Chicken Curry | restaurant101 | 25.00 | 20 |
| meal102 | Vegetable Curry | restaurant101 | 18.00 | 20 |

## API

Below is a list of API endpoints with their respective input and output. Please note that the application needs to be running for the following endpoints to work. For more information about how to run the application, please refer to run the application section above.

### Add Product to Cart
```http
POST /cart/product
Content-Type: application/json
```

Request Body (`quantity` must be greater than zero)
```json
{
  "userId": "user101",
  "productId": "product101",
  "outletId": "store101",
  "quantity": 2
}
```

Returns the added `CartItem` directly with HTTP 200. Example for a unit price of 10.50:
```json
{
  "productId": "product101",
  "productName": "Wheat Bread",
  "quantity": 2,
  "unitPrice": 10.50,
  "totalPrice": 21.00
}
```

Adding a product checks the combined quantity already in the cart against available stock.
It does not reserve or decrease stock. Each successful addition creates a cart item;
its response contains the quantity from that addition.

For a restaurant meal, the same endpoint also accepts `spiceLevel` and
`utensilsNeeded`:

```json
{
  "userId": "user101",
  "outletId": "restaurant101",
  "productId": "meal101",
  "quantity": 2,
  "spiceLevel": "MEDIUM",
  "utensilsNeeded": true
}
```

The added item is returned directly:

```json
{
  "productId": "meal101",
  "productName": "Chicken Curry",
  "quantity": 2,
  "unitPrice": 25.00,
  "totalPrice": 50.00,
  "spiceLevel": "MEDIUM",
  "utensilsNeeded": true
}
```

`spiceLevel` accepts `NONE`, `MILD`, `MEDIUM`, or `HOT` and defaults to `NONE`.
`utensilsNeeded` defaults to `false`. Choices apply to every unit in that cart
item and do not change its price. Separate additions can have different choices;
stock checks include the quantities of all items for the same product.

Grocery items omit both customization fields from responses. Providing either
non-null option for groceries, including `utensilsNeeded: false`, returns HTTP 400.
Unknown spice values also return HTTP 400.

An empty cart takes its outlet from the first successful addition. While it
contains items, additions from another store or restaurant return HTTP 400.
A rejected addition leaves both the existing items and outlet unchanged.

If the combined quantity exceeds available stock, both cart addition and checkout
return HTTP 409 with a message such as:

```json
{
  "message": "Requested quantity 21 exceeds available stock 20 for product meal101"
}
```

### View Cart
```http
GET /cart/view?userId=user101
```

Returns the complete `Cart` directly with HTTP 200:

| Field | Contents |
|-------|----------|
| `cartId` | Cart identifier |
| `outlet` | Store or restaurant, including inventory where exposed by the outlet; initially `null` for a new empty cart |
| `cartItems` | Items with the same fields as the add-product response above |
| `user` | Cart owner; the user's cart back-reference is omitted |

Cart items retain their captured names and unit prices. Viewing a cart does not
look up its items again in the product catalog.

For a valid user, the cart is created lazily on first access with a generated UUID,
the correct owner and no outlet. Later requests reuse the same cart. An unknown
user returns HTTP 404 and does not create a cart. Seed data supplies users and
catalogs; cart storage belongs to the in-memory cart repository.

Viewing returns a detached cart with an immutable snapshot of its item list.
Additions and checkout coordinate on the same cart lock, so concurrent requests
cannot bypass that cart's outlet or combined-quantity checks.

### Place Order

```http
POST /orders
Content-Type: application/json
```

Request Body
```json
{
  "userId": "user101"
}
```

Checkout requires a nonempty cart and rechecks product availability and prices.
It copies the items into an order, preserving each meal's spice level and utensils
selection. Grocery order items omit both customization fields.

Checkout of an empty cart returns HTTP 409 with a clear error message.

A successful checkout returns HTTP 201 with a `Location: /orders/{orderId}` header
and the order directly in the response body. Example after adding two Chicken Curry
meals with medium spice and utensils requested (ID and timestamp are generated):

```json
{
  "orderId": "7e5f6e92-21c0-4b72-9b64-778fbf2957b5",
  "userId": "user101",
  "outletId": "restaurant101",
  "items": [
    {
      "productId": "meal101",
      "productName": "Chicken Curry",
      "quantity": 2,
      "unitPrice": 25.00,
      "totalPrice": 50.00,
      "spiceLevel": "MEDIUM",
      "utensilsNeeded": true
    }
  ],
  "createdAt": "2026-11-16T10:00:00Z",
  "status": "PLACED",
  "totalPrice": 50.00
}
```

After the order is saved, the cart's items are cleared. The empty cart can then
accept products from another outlet. Later cart changes do not alter the saved order.

Cart locking applies within one application instance. Stock is checked but is not
reserved or deducted; preventing overselling across different customers requires
a separate atomic inventory operation.

### View Order

```http
GET /orders/{orderId}
```

Returns the saved order directly with HTTP 200, including item quantities, prices,
meal customizations, total and current status. An unknown order ID returns HTTP 404.

### Inventory Health
```http
GET /inventory/health?storeid=<storeid>
```

Response Body 
```json lines
{
    // to be implemented.
}
```

## Tech Requirements
The project requires Java 25. If you have multiple JVMs on your machine, you might want to
consider using a tool such as [sdkman](https://sdkman.io/) to handle switching between versions.

The project makes use of Gradle and uses the Gradle wrapper, which means you don't need Gradle installed.

### Installing Java

Install java using homeBrew
```console
brew install openjdk@25
```

Installing java on Windows, [refer](https://www.java.com/en/download/help/windows_manual_download.html#xd_co_f=NzA3YTZmNzAtOTEzMS00OWFiLTk2NjUtODg0NjNhMjRhMjkw~)

Other ways to Download and install java, [refer](https://www.oracle.com/in/java/technologies/downloads/#java25).

#### Verify Java Version Installed
```console
java -version
```

### Useful Gradle commands
#### Build the project
Compiles the project, runs the test and then creates an executable JAR file
```bash
$ ./gradlew build
```

#### Run the application
Run the application which will be listening on port ```8080```.
```bash
$ ./gradlew bootRun
```

Run the application using Java and the executable JAR file produced by the Gradle ```build``` task. The application will be listening on port ```8080```.
```bash
$ java -jar  build/libs/joi-delivery-java.jar
```

#### Run the tests
There are two types of tests, the unit tests and the functional tests. These can be executed as follows.

* Run unit tests only
```bash
$ ./gradlew test
```
* Run both unit and functional tests
```bash
$ ./gradlew check
```
#### List all Gradle tasks
List all the tasks that Gradle can run, such as ```build ``` and ```test```.
```bash
$ ./gradlew tasks
```

> Note: We are currently using Java 25, which is the current LTS version.
