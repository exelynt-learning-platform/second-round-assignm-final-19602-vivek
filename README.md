# second-round-assignm-final-19602-vivek
Final Project Assignment - This repository contains the complete final project code and documentation.



documantation of e-commerce-backend-springboot

What this project is

This is a Spring Boot REST API for a small online shop. Users can register and log in (JWT), browse products, manage a shopping cart, place orders, and pay with Stripe Checkout. Admins can manage the product catalog. Data is stored in MySQL; passwords are hashed; the API uses DTOs (not raw entities) and a global error format.

Base URL (local): http://localhost:8080
Auth header (when required): Authorization: Bearer <JWT>

Local setup

Prerequisites

Java 17+
Maven (or your IDE’s embedded Maven)
MySQL running locally, with a database the app can use (name must match application.yml, e.g. ecommerce_db)

Steps

Create the database in MySQL (or ensure createDatabaseIfNotExist / your URL matches an existing DB).
Configure src/main/resources/application.yml:
spring.datasource.url, username, password for MySQL.

jwt secret (or set JWT_SECRET in the environment).
Stripe: stripe.secret-key (and optional stripe.webhook-secret for webhooks).
After changing YAML, run mvn clean compile (or rebuild in the IDE) so target/classes is up to date.
Build & run from the project root:
mvn spring-boot:run
or run com.ecommerce.EcommerceApplication from your IDE.

Admin user: new users register as USER. To manage products, set role = ADMIN for a user in the database (or your seed SQL).
Stripe webhooks (optional): for order status PAID after payment, use Stripe CLI:
stripe listen --forward-to localhost:8080/payments/webhook and set the printed whsec_... as stripe.webhook-secret (or STRIPE_WEBHOOK_SECRET).


Security overview
Access	Endpoints

No login	/auth/register, /auth/login, GET /products, GET /products/{id}, POST /payments/webhook

Logged-in user (JWT)	Cart, orders, POST /payments/create-session
Admin (JWT + ROLE_ADMIN)	POST/PUT/DELETE on /products


All endpoints (quick reference)

Method	Path	Auth
POST	/auth/register	No
POST	/auth/login	No
GET	/products	No
GET	/products/{id}	No
POST	/products	Admin
PUT	/products/{id}	Admin
DELETE	/products/{id}	Admin
GET	/cart	User
POST	/cart/add	User
PUT	/cart/update	User
DELETE	/cart/remove	User
POST	/orders/create	User
GET	/orders	User
GET	/orders/{id}	User
POST	/payments/create-session	User
POST	/payments/webhook	No (Stripe signature)


Endpoint explanations

Authentication
POST /auth/register — Creates a user (name, email, password). Returns a JWT and user info. Password must meet validation rules (e.g. min length).

POST /auth/login — Email + password. Returns a JWT on success. Use this token for all protected calls.


Products (/products)
GET /products — Paginated product list. Query params: page, size, sort (Spring Data format, e.g. sort=name,asc). Default paging/sort is set in code.

GET /products/{id} — One product by ID.
POST /products — Admin only. Create a product (name, description, price, stock, image URL).

PUT /products/{id} — Admin only. Update that product (same kind of body as create).

DELETE /products/{id} — Admin only. Deletes the product.


Cart (/cart)
GET /cart — Returns the current user’s cart with line items and totals. Creates a cart if needed when adding items elsewhere.

POST /cart/add — Add a product by productId and quantity. Quantity cannot exceed stock.

PUT /cart/update — Set quantity for a line identified by productId.

DELETE /cart/remove — Remove a line by productId (JSON body, not query params).

Orders (/orders)
POST /orders/create — Builds an order from the current cart (shipping address in body), then empties the cart. Order starts as CREATED; stock is reduced when payment succeeds via Stripe webhook.

GET /orders — Lists only the logged-in user’s orders.

GET /orders/{id} — One order only if it belongs to the logged-in user.

Payments (/payments)
POST /payments/create-session — Body: { "orderId": <number> }. Must be your order in CREATED status. Returns Stripe Checkout URL to open in a browser. Requires valid Stripe secret key in config.

POST /payments/webhook — Called by Stripe (not usually by Postman). Sends raw event body + Stripe-Signature header. Marks orders PAID or FAILED and updates stock. Needs correct webhook signing secret.
Errors

Validation and business errors usually return JSON like:
{ "status": "error", "message": "..." }
401 / 403 are used when not logged in or wrong role (see security config).

Postman / testing tip
POST /auth/register or /auth/login → copy token.
Set header: Authorization: Bearer <token>.
Then call cart → add → orders/create → payments/create-session → open checkout URL.