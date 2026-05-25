CREATE TABLE IF NOT EXISTS `wallets` (
  `id` INTEGER NOT NULL PRIMARY KEY,
  `name` TEXT NOT NULL,
  `currency` TEXT NOT NULL,
  `created_at` INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS `accounts` (
  `id` INTEGER NOT NULL PRIMARY KEY,
  `name` TEXT NOT NULL,
  `type` TEXT NOT NULL,
  `credit_limit` REAL NOT NULL DEFAULT 0.0,
  `payment_due_day` INTEGER NOT NULL DEFAULT 0,
  `is_hidden` INTEGER NOT NULL DEFAULT 0,
  `created_at` INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS `categories` (
  `id` INTEGER NOT NULL PRIMARY KEY,
  `name` TEXT NOT NULL,
  `type` TEXT NOT NULL,
  `created_at` INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS `transactions` (
  `id` INTEGER NOT NULL PRIMARY KEY,
  `amount` REAL NOT NULL,
  `type` TEXT NOT NULL,
  `wallet_id` INTEGER NOT NULL,
  `account_id` INTEGER NOT NULL,
  `category_id` INTEGER NOT NULL,
  `date` INTEGER NOT NULL,
  `note` TEXT,
  `created_at` INTEGER NOT NULL
);
