-- Initialize the app schema in a new, empty PostgreSQL 14+ database.
-- Schema only: no source data, role ownership or grants. Apply once with ON_ERROR_STOP.

BEGIN;
SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

CREATE SCHEMA app;

SET default_tablespace = '';

SET default_table_access_method = heap;

CREATE TABLE app.cart_items (
    id integer NOT NULL,
    cart_id integer NOT NULL,
    watch_id integer NOT NULL,
    quantity integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_cart_item_quantity CHECK ((quantity > 0))
);

ALTER TABLE app.cart_items ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME app.cart_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE app.carts (
    id integer NOT NULL,
    user_id integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE app.carts ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME app.carts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE app.categories (
    id integer NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE app.categories ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME app.categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE app.order_items (
    id integer NOT NULL,
    order_id integer NOT NULL,
    watch_id integer NOT NULL,
    quantity integer NOT NULL,
    unit_price numeric(12,0) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_order_item_price CHECK ((unit_price >= (0)::numeric)),
    CONSTRAINT chk_order_item_quantity CHECK ((quantity > 0))
);

ALTER TABLE app.order_items ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME app.order_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE app.orders (
    id integer NOT NULL,
    user_id integer NOT NULL,
    recipient_name character varying(100) NOT NULL,
    recipient_phone character varying(20) NOT NULL,
    shipping_address text NOT NULL,
    total_amount numeric(12,0) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    payment_method character varying(20) DEFAULT 'COD'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_order_payment_method CHECK (((payment_method)::text = 'COD'::text)),
    CONSTRAINT chk_order_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'SHIPPING'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT chk_order_total CHECK ((total_amount >= (0)::numeric))
);

ALTER TABLE app.orders ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME app.orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE app.payments (
    id integer NOT NULL,
    order_id integer NOT NULL,
    amount numeric(12,0) NOT NULL,
    method character varying(20) DEFAULT 'COD'::character varying NOT NULL,
    status character varying(20) DEFAULT 'UNPAID'::character varying NOT NULL,
    paid_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_paid_at CHECK (((((status)::text = 'UNPAID'::text) AND (paid_at IS NULL)) OR (((status)::text = 'PAID'::text) AND (paid_at IS NOT NULL)))),
    CONSTRAINT chk_payment_amount CHECK ((amount >= (0)::numeric)),
    CONSTRAINT chk_payment_method CHECK (((method)::text = 'COD'::text)),
    CONSTRAINT chk_payment_status CHECK (((status)::text = ANY ((ARRAY['UNPAID'::character varying, 'PAID'::character varying])::text[])))
);

ALTER TABLE app.payments ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME app.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE app.users (
    id integer NOT NULL,
    full_name character varying(100) NOT NULL,
    email character varying(150) NOT NULL,
    password_hash character varying(255) NOT NULL,
    phone character varying(20),
    role character varying(20) DEFAULT 'CUSTOMER'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_user_role CHECK (((role)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'ADMIN'::character varying])::text[])))
);

ALTER TABLE app.users ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME app.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE TABLE app.watches (
    id integer NOT NULL,
    category_id integer NOT NULL,
    name character varying(150) NOT NULL,
    brand character varying(100) NOT NULL,
    model_number character varying(100) NOT NULL,
    price numeric(12,0) NOT NULL,
    stock_quantity integer DEFAULT 0 NOT NULL,
    watch_type character varying(20) NOT NULL,
    movement_type character varying(100),
    battery_life_hours integer,
    description text,
    image_url text,
    available boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_watch_price CHECK ((price >= (0)::numeric)),
    CONSTRAINT chk_watch_stock CHECK ((stock_quantity >= 0)),
    CONSTRAINT chk_watch_subtype CHECK (((((watch_type)::text = 'AUTOMATIC'::text) AND (movement_type IS NOT NULL) AND (battery_life_hours IS NULL)) OR (((watch_type)::text = 'SMART'::text) AND (movement_type IS NULL) AND (battery_life_hours IS NOT NULL) AND (battery_life_hours > 0)))),
    CONSTRAINT chk_watch_type CHECK (((watch_type)::text = ANY ((ARRAY['AUTOMATIC'::character varying, 'SMART'::character varying])::text[])))
);

ALTER TABLE app.watches ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME app.watches_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE ONLY app.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY app.carts
    ADD CONSTRAINT carts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY app.carts
    ADD CONSTRAINT carts_user_id_key UNIQUE (user_id);

ALTER TABLE ONLY app.categories
    ADD CONSTRAINT categories_name_key UNIQUE (name);

ALTER TABLE ONLY app.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY app.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);

ALTER TABLE ONLY app.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY app.payments
    ADD CONSTRAINT payments_order_id_key UNIQUE (order_id);

ALTER TABLE ONLY app.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);

ALTER TABLE ONLY app.cart_items
    ADD CONSTRAINT uq_cart_watch UNIQUE (cart_id, watch_id);

ALTER TABLE ONLY app.order_items
    ADD CONSTRAINT uq_order_watch UNIQUE (order_id, watch_id);

ALTER TABLE ONLY app.users
    ADD CONSTRAINT users_email_key UNIQUE (email);

ALTER TABLE ONLY app.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

ALTER TABLE ONLY app.watches
    ADD CONSTRAINT watches_model_number_key UNIQUE (model_number);

ALTER TABLE ONLY app.watches
    ADD CONSTRAINT watches_pkey PRIMARY KEY (id);

ALTER TABLE ONLY app.cart_items
    ADD CONSTRAINT fk_cart_item_cart FOREIGN KEY (cart_id) REFERENCES app.carts(id) ON DELETE CASCADE;

ALTER TABLE ONLY app.cart_items
    ADD CONSTRAINT fk_cart_item_watch FOREIGN KEY (watch_id) REFERENCES app.watches(id);

ALTER TABLE ONLY app.carts
    ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES app.users(id);

ALTER TABLE ONLY app.order_items
    ADD CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES app.orders(id);

ALTER TABLE ONLY app.order_items
    ADD CONSTRAINT fk_order_item_watch FOREIGN KEY (watch_id) REFERENCES app.watches(id);

ALTER TABLE ONLY app.orders
    ADD CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES app.users(id);

ALTER TABLE ONLY app.payments
    ADD CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES app.orders(id);

ALTER TABLE ONLY app.watches
    ADD CONSTRAINT fk_watch_category FOREIGN KEY (category_id) REFERENCES app.categories(id);
COMMIT;
