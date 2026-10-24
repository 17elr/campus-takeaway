ALTER TABLE users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE cart (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL UNIQUE, shop_id BIGINT NULL,
  version BIGINT NOT NULL DEFAULT 0, checkout_key VARCHAR(36) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id), FOREIGN KEY (shop_id) REFERENCES shops(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE cart_item (
  cart_id BIGINT NOT NULL, dish_id BIGINT NOT NULL, quantity INT NOT NULL,
  PRIMARY KEY (cart_id,dish_id), CHECK (quantity BETWEEN 1 AND 99),
  FOREIGN KEY (cart_id) REFERENCES cart(id), FOREIGN KEY (dish_id) REFERENCES dishes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE orders
  ADD COLUMN request_key VARCHAR(36) NULL,
  ADD COLUMN checkout_cart_version BIGINT NULL,
  ADD COLUMN checkout_address_id BIGINT NULL,
  ADD COLUMN request_hash CHAR(64) NULL,
  ADD COLUMN expires_at TIMESTAMP NULL,
  ADD CONSTRAINT uq_student_request UNIQUE (student_id,request_key),
  ADD INDEX ix_order_expiry (status,expires_at,id),
  ADD INDEX ix_order_student (student_id,id),
  ADD INDEX ix_order_shop (shop_id,id),
  ADD INDEX ix_order_rider (rider_id,id);
UPDATE orders SET expires_at=DATE_ADD(created_at,INTERVAL 15 MINUTE) WHERE status='UNPAID';
ALTER TABLE order_events ADD COLUMN reason VARCHAR(40) NULL;
