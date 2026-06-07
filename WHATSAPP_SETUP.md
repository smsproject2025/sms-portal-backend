# WhatsApp Business API Setup Guide

Complete step-by-step guide to connect SMSPortal to the **Meta (WhatsApp) Cloud API**.

---

## Architecture Overview

```
Customer's Phone
      │  sends message
      ▼
Meta WhatsApp Cloud API
      │  webhook POST
      ▼
SMSPortal Backend  ─── RabbitMQ ───▶  WhatsApp Gateway Service
      │                                       │
      ▼                                       ▼
  MySQL DB                             Meta Cloud API
  (logs, contacts,                     (sends messages)
   templates, campaigns)
```

---

## Step 1 — Create a Meta Developer App

1. Go to **https://developers.facebook.com**
2. Click **My Apps → Create App**
3. Choose **"Business"** as the app type
4. Fill in app name (e.g. `SMSPortal`) and business email
5. Click **Create App**

---

## Step 2 — Add WhatsApp Business Product

1. In your app dashboard, scroll to **"Add Products to Your App"**
2. Find **WhatsApp** → click **Set Up**
3. You'll land on the **WhatsApp → Getting Started** page

---

## Step 3 — Get Your Credentials

On the **WhatsApp → API Setup** page, copy:

| Credential | Where to find | Config key |
|---|---|---|
| **Temporary Access Token** | "Access Token" box | `whatsapp.access-token` |
| **Phone Number ID** | "From" phone number dropdown | `whatsapp.phone-number-id` |
| **WhatsApp Business Account ID** | Top of the page | (for reference) |

> ⚠️ The temporary token expires in **24 hours**. For production, generate a permanent system user token (see Step 7).

---

## Step 4 — Update application.yml

```yaml
whatsapp:
  access-token: EAAxxxxxxxxxxxxxx   # paste your token here
  phone-number-id: "123456789012345" # paste Phone Number ID here
  api-version: v19.0
  webhook-verify-token: my_secure_random_string_123
  mock: false                        # set to true for dev without real API
```

---

## Step 5 — Register Webhook

Meta needs to verify your server before sending webhooks.

### If running locally (use ngrok):

```bash
# Install ngrok: https://ngrok.com/download
ngrok http 8080

# You'll get a URL like:
# https://abc123.ngrok.io
```

### In Meta Developer Console:

1. Go to **WhatsApp → Configuration**
2. Under **Webhooks**, click **Edit**
3. Set:
   - **Callback URL**: `https://abc123.ngrok.io/api/whatsapp/webhook`
   - **Verify Token**: same value as `whatsapp.webhook-verify-token` in your yml
4. Click **Verify and Save**
5. Subscribe to these fields: `messages`, `message_deliveries`, `message_reads`

### If running in production:

Use your real domain:
`https://yourdomain.com/api/whatsapp/webhook`

---

## Step 6 — Send Your First Test Message

```bash
# Test text message
curl -X POST http://localhost:8080/api/whatsapp/send/text \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "mobiles": ["919876543210"],
    "message": "Hello from SMSPortal! 🎉"
  }'
```

---

## Step 7 — Generate Permanent Access Token (Production)

1. Go to **Business Settings** → **Users** → **System Users**
2. Create a **System User** with Admin role
3. Click **Generate New Token**
4. Select your app and grant `whatsapp_business_messaging` permission
5. Copy the token → set in `whatsapp.access-token`

---

## Step 8 — Add Production Phone Number

1. Go to **WhatsApp → Phone Numbers** → **Add Phone Number**
2. Choose a **business phone number** (must not be registered on personal WhatsApp)
3. Verify via OTP
4. Update `whatsapp.phone-number-id` with the new number's ID

---

## Step 9 — Create & Submit Message Templates

Templates are required for outbound marketing/utility messages to customers who haven't messaged you in the last 24 hours.

### Via SMSPortal UI:
1. Go to **WhatsApp → Templates** tab
2. Fill in template name, category, language, body
3. Click **Submit for Approval**
4. Wait 24–48 hours for Meta review

### Template body example:
```
Hello {{1}}, your order {{2}} has been confirmed! 
Expected delivery: {{3}}.
Track here: {{4}}
```

### Category guide:
| Category | Use case | Pricing |
|---|---|---|
| UTILITY | Order updates, receipts, alerts | Lower |
| MARKETING | Promotions, offers | Higher |
| AUTHENTICATION | OTPs, verification codes | Lowest |

---

## Step 10 — Opt-In Management

WhatsApp **requires** you to get customer consent before messaging them.

### Add contacts with opt-in:
```bash
POST /api/whatsapp/contacts
{
  "mobile": "919876543210",
  "name": "Rahul Sharma",
  "tags": "newsletter,vip"
}
```

### Import CSV:
```
mobile,name,email,tags
919876543210,Rahul Sharma,rahul@example.com,"vip,offers"
918123456789,Priya Patel,priya@example.com,"newsletter"
```

Upload via **WhatsApp → Contacts** tab → **Bulk Import**.

---

## Feature Reference

| Feature | Tab | API Endpoint |
|---|---|---|
| Send text message | Send | `POST /api/whatsapp/send/text` |
| Send template | Send | `POST /api/whatsapp/send/template` |
| Send image/doc/video | Send | `POST /api/whatsapp/send/media` |
| View delivery logs | Logs | `GET /api/whatsapp/logs` |
| Delivery stats | — | `GET /api/whatsapp/stats` |
| Manage contacts | Contacts | `GET/POST /api/whatsapp/contacts` |
| Import contacts CSV | Contacts | `POST /api/whatsapp/contacts/import` |
| Create campaign | Campaigns | `POST /api/whatsapp/campaigns` |
| Launch campaign | Campaigns | `POST /api/whatsapp/campaigns/{id}/launch` |
| Create template | Templates | `POST /api/whatsapp/templates` |
| View inbox | Inbox | `GET /api/whatsapp/inbox` |
| Unread count | Inbox | `GET /api/whatsapp/inbox/unread-count` |
| Delivery webhook | — | `POST /api/whatsapp/webhook` |

---

## Message Pricing (SMSPortal)

| Type | Rate per message |
|---|---|
| Text | ₹0.40 |
| Template | ₹0.55 |
| Media (Image/Video/Doc) | ₹0.60 |

> Meta also charges separately based on conversation type and country. See https://developers.facebook.com/docs/whatsapp/pricing

---

## Mock Mode (Development)

Set `whatsapp.mock: true` in `application.yml` to test without real Meta credentials. All messages will be logged to console and marked as SENT without any actual API call.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `401 Unauthorized` from Meta | Access token expired — regenerate it |
| Webhook verify fails | Check `webhook-verify-token` matches in Meta console and yml |
| Template rejected | Review Meta's content policy; avoid promotional content in UTILITY templates |
| Message not delivered | Check if number is a valid WhatsApp number; use Meta's test number |
| `131030` error from Meta | Recipient phone number not on WhatsApp |
| `100` error from Meta | Invalid phone number format — use E.164 (91xxxxxxxxxx) |
| Webhook not receiving | Check ngrok is running; check firewall rules in production |
