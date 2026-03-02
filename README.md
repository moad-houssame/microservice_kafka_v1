# 🚀 Microservices Kafka - Exactly Once Payment Processing

## 📋 Vue d'ensemble

Architecture microservices pour le traitement de paiements avec garantie **Exactly Once Processing**.

```
┌─────────────┐      ┌──────────────────┐      ┌────────────┐
│   Client    │─────▶│  Payment Service │─────▶│ PostgreSQL │
│  (App/Web)  │      │   (Spring Boot)  │      │  (Storage) │
└─────────────┘      └────────┬─────────┘      └────────────┘
                              │
                              ▼
                     ┌────────────────┐
                     │     Kafka      │
                     │ (Event Stream) │
                     └────────┬───────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │ Notification Service │
                   │    (Email/SMS)       │
                   └─────────────────────┘
```

## 🛡️ Exactly Once Processing - Comment c'est garanti ?

### 1. **Idempotence au niveau Application**
```java
// Vérification avant traitement
if (paymentRepository.existsByOrderId(orderId)) {
    return "Payment already processed";
}
```

### 2. **Contrainte UNIQUE en Base de Données**
```sql
ALTER TABLE payments ADD CONSTRAINT unique_order_id UNIQUE (order_id);
```

### 3. **Transactions Atomiques (DB + Outbox)**
```java
@Transactional  // DB + outbox dans une seule transaction
public PaymentResponse processPayment(PaymentRequest request) {
    // ...
}
```

### 4. **Publication Kafka via Outbox**
```java
// L'evenement est stocke en outbox puis publie par un scheduler
outboxService.enqueuePaymentEvent(paymentEvent);
```

### 5. **Kafka Idempotent Producer**
```yaml
spring.kafka.producer.properties.enable.idempotence: true
```

## 🏗️ Structure du Projet

```
microservices-kafka/
├── docker-compose.yml          # Infrastructure Docker
├── payment_service/            # 💳 Service de paiement
│   ├── src/main/java/com/example/payment/
│   │   ├── config/             # Configuration Kafka
│   │   ├── controller/         # API REST
│   │   ├── dto/                # Request/Response DTOs
│   │   ├── entity/             # Entités JPA
│   │   ├── exception/          # Gestion des erreurs
│   │   ├── kafka/              # Producer Kafka
│   │   ├── repository/         # Accès PostgreSQL
│   │   └── service/            # Logique métier
│   └── Dockerfile
└── notification_service/       # 🔔 Service de notification
    ├── src/main/java/com/example/notification/
    │   ├── config/             # Configuration Consumer
    │   ├── controller/         # Health check
    │   ├── event/              # PaymentEvent DTO
    │   ├── kafka/              # Consumer Kafka
    │   └── service/            # Envoi notifications
    └── Dockerfile
```

## 🚀 Démarrage Rapide

### 1. Démarrer l'infrastructure Docker
```bash
docker-compose up -d
```

### 2. Démarrer les services (développement local)
```bash
# Terminal 1 - Payment Service
cd payment_service
./mvnw spring-boot:run

# Terminal 2 - Notification Service
cd notification_service
./mvnw spring-boot:run
```

### 3. Tester un paiement
```bash
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-123",
    "amount": 100,
    "userId": "U-99"
  }'
```

## 📡 API Endpoints

### Payment Service (Port 8081)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/api/payments` | Créer un paiement |
| `GET` | `/api/payments` | Liste tous les paiements |
| `GET` | `/api/payments/{id}` | Paiement par ID |
| `GET` | `/api/payments/order/{orderId}` | Paiement par orderId |
| `GET` | `/api/payments/user/{userId}` | Paiements d'un utilisateur |

### Notification Service (Port 8082)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/notifications` | Liste des notifications (filtres: `userId`, `orderId`, `limit`) |
| `DELETE` | `/api/notifications` | Supprime toutes les notifications ou filtrees |
| `GET` | `/api/notifications/health` | Health check |

## 📡 Swagger UI

- Gateway (unique): http://localhost:8080/swagger-ui.html
- Payment Service: http://localhost:8081/swagger-ui.html
- Notification Service: http://localhost:8082/swagger-ui.html

## 🧪 Tests Exactly Once avec JMeter

### Scénario de test
1. Envoyer 1000 requêtes simultanées avec le même `orderId`
2. Vérifier qu'un seul paiement est enregistré
3. Vérifier qu'un seul événement Kafka est envoyé

### Résultat attendu
- ✅ 1 seul enregistrement en base de données
- ✅ 1 seul événement Kafka publié
- ✅ 1 seule notification envoyée
- ✅ 0 doublon

## 🔄 Flux de Traitement d'un Paiement

```
[1] Client clique "Pay"
        ↓
[2] POST /payments (Controller)
        ↓
[3] PaymentService.processPayment()
        ↓
[4] Vérification doublon (Idempotence)
        ↓
[5] Sauvegarde PostgreSQL (Transaction)
        ↓
[6] Enqueue event outbox (Transaction)
        ↓
[7] OutboxPublisher publie l'event Kafka
        ↓
[8] Notification Service consomme l'event
        ↓
[9] Email / SMS envoyé
        ↓
[10] Paiement traité UNE SEULE FOIS (Exactly Once) ✅
```

## 📊 Technologies Utilisées

- **Java 21** - Langage de programmation
- **Spring Boot 3.2** - Framework
- **Spring Kafka** - Intégration Kafka
- **PostgreSQL 15** - Base de données
- **Apache Kafka** - Event Streaming
- **Docker** - Conteneurisation
- **Lombok** - Réduction boilerplate

## 🎓 Concepts Clés pour l'Examen

| Concept | Description |
|---------|-------------|
| **Microservice** | Service indépendant, déployable séparément |
| **Event-Driven** | Communication asynchrone via événements |
| **Exactly Once** | Garantie de traitement unique |
| **Idempotence** | Même résultat si exécuté plusieurs fois |
| **Transaction** | Opération atomique (tout ou rien) |

## 🏢 Cas d'usage Enterprise

Cette architecture est utilisée par :
- **Fintech** (Stripe, PayPal)
- **Banking** (Virements, Paiements)
- **E-commerce** (Amazon, Uber)
- **P2S** - Paiement digital

## 📝 Exemple de Requête/Réponse

### Requête
```json
POST /api/payments
{
  "orderId": "ORD-123",
  "amount": 100,
  "userId": "U-99"
}
```

### Réponse (Nouveau paiement - 201 Created)
```json
{
  "paymentId": 1,
  "orderId": "ORD-123",
  "userId": "U-99",
  "amount": 100,
  "status": "SUCCESS",
  "message": "Paiement traité avec succès",
  "timestamp": "2024-01-15T10:30:00",
  "alreadyProcessed": false
}
```

### Réponse (Paiement existant - 200 OK, Exactly Once)
```json
{
  "paymentId": 1,
  "orderId": "ORD-123",
  "userId": "U-99",
  "amount": 100,
  "status": "SUCCESS",
  "message": "Paiement déjà traité (Exactly Once)",
  "timestamp": "2024-01-15T10:30:05",
  "alreadyProcessed": true
}
```

## 📧 Notification Reçue

Quand le Notification Service consomme l'événement, il persiste une notification en base et elle est consultable via l'API.

```
╔══════════════════════════════════════════╗
║     🎉 CONFIRMATION DE PAIEMENT 🎉        ║
╠══════════════════════════════════════════╣
║  Commande: ORD-123                       ║
║  Montant:  100 MAD                       ║
║  Statut:   SUCCESS                       ║
╠══════════════════════════════════════════╣
║  Votre paiement de 100 MAD               ║
║  a été traité avec succès.               ║
║                                          ║
║  Merci pour votre confiance!             ║
╚══════════════════════════════════════════╝

```

### Exemple d'API Notification
```bash
curl "http://localhost:8082/api/notifications?limit=20"
curl "http://localhost:8082/api/notifications?userId=U-99"
curl "http://localhost:8082/api/notifications?orderId=ORD-123"
curl -X DELETE "http://localhost:8082/api/notifications"
```

## 🔁 Pipeline Kafka (POC)

Parcours:
`orders-topic` → **payment-service** (read/process/write) → `payments-topic` → **notification-service** (read/process/write) → `notifications-topic`

## ✅ Format de messages (Avro par défaut)

Par défaut, le pipeline utilise **Avro** via Schema Registry.

Pour repasser en **JSON**, il suffit de définir :

```yaml
app:
  kafka:
    message-format: json
```

En Avro (par défaut) :

```yaml
app:
  kafka:
    message-format: avro
```

Schema Registry (Docker) : http://localhost:8085

### Observabilite (POC)

- Jaeger: `http://localhost:16686`
- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Loki: `http://localhost:3100`

### Script de demo

```
./scripts/demo-pipeline.ps1 -OrderId "order-001"
```
