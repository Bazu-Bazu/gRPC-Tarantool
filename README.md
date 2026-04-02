# gRPC KV Storage

Простой и производительный key-value сервис на Java + gRPC с использованием Tarantool 3.2 в качестве хранилища.

## 📌 Возможности API

- put(key, value)	- Сохраняет или обновляет значение
- get(key) - Получает значение по ключу
- delete(key) -	Удаляет ключ
- range(key_since, key_to) - Возвращает stream ключ-значение
- count()	- Количество записей

Методы **put** и **get** корректно работают с **null** значениями в поле **value**.

## 🛠 Сборка проекта

```
mvn clean install
```

## 🐳 Запуск через Docker

```
docker-compose up --build
```

gRPC сервис будет доступен на:

```
localhost:9090
```

Tarantool на:

```
localhost:3301
```

 
