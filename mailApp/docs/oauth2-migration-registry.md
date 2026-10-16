# OAuth2 Outbound Migration Registry

> **Purpose** — Single source of truth for the auth-method status of every outbound HTTP call made by this repository (`mailApp`).
> **Source of truth** — Derived exclusively from the current Java source code. Treat any discrepancy with older documents as a bug in the older document.
>
> **Workflow**
> 1. Pick any URL from the **🔴 To Be Migrated** table.
> 2. Prompt: _"migrate `<bean/endpoint>`"_.
> 3. A migration plan will be shared for your review, then implemented on approval.
> 4. On completion the row moves to **✅ OAuth2 Compatible** and this file is updated.
>
> **Rule** — Hand-rolled OAuth2 callers are **not** migrated unless explicitly instructed. The `Oauth2TokenService` (backed by `OAuth2AuthorizedClientManager`) is the only mechanism used when migrating legacy-auth endpoints.

---

## ✅ OAuth2 Compatible

These endpoints already obtain a Bearer token before calling the target URL.

| # | Bean / JNDI Name | Actual URL (train1 / NP) | Auth Mechanism | Token Source | Caller Class(es) | Notes |
|---|---|---|---|---|---|---|
| 1 | `pomUrl` | `https://corso.api-np.swissre.com/pom-api/train1/policies/{policyNum}?format=LEGACY` | **New Infra OAuth2** | `Oauth2TokenService.getAccessToken(OAuth2ClientConfig.POM_DEFAULT)` | `MailHelperBO.sendMailNotification()` → `restService.getCallOAuth2(pomUrl + policyNum + "?format=LEGACY", POM_DEFAULT)` | Re-migrated 2026-06-02; `pomUrl` JNDI updated to Corso API gateway across all envs |
| 2 | `dealUrl` | `https://uwbench-train1.swissreapps-np.com/deal/api/Deals/{dealNum}` | **New Infra OAuth2** | `Oauth2TokenService.getAccessToken(OAuth2ClientConfig.UWB_DEFAULT)` | `MailHelperBO.setAttributes(dealNum)` → `restService.getCallOAuth2(url.toString(), UWB_DEFAULT)` | Migrated 2026-04-03; single `setAttributes()` fix covers rows 2 & 3 |
| 3 | `dealUrl` | `https://uwbench-train1.swissreapps-np.com/deal/api/Deals/{dealOrPolicyNum}` | **New Infra OAuth2** | `Oauth2TokenService.getAccessToken(OAuth2ClientConfig.UWB_DEFAULT)` | `MailHelperBO.sendDeclineDealOrPolicyMailNotification()` → `setAttributes()` → `restService.getCallOAuth2(url.toString(), UWB_DEFAULT)` | Migrated 2026-04-03; covered by the same `setAttributes()` fix as row 2 |

---

## 🔴 To Be Migrated

These endpoints currently authenticate via LTPA or Basic Auth.

| # | Bean / JNDI Name | Actual URL (train1 / NP) | Current Auth | Caller Class(es) | Method(s) to Change | Target Registration | Priority |
|---|---|---|---|---|---|---|---|

> *(All endpoints migrated — table is empty.)*

---

## ⛔ Explicitly Excluded

These endpoints use legacy auth intentionally and are out of scope for OAuth2 migration.

| # | Bean / JNDI Name | Actual URL | Auth Mechanism | Caller Class(es) | Reason Excluded |
|---|---|---|---|---|---|
| 1 | SMTP relay (`exmailrelay.int.swissre.com:25`) | `smtp://exmailrelay.int.swissre.com:25` | SMTP AUTH (`mailTechUser` / `mailTechPass`) — configured in `JavaMailSenderImpl` | `MailBO.sendMail()` via `JavaMailSender.send()` | SMTP relay does not support OAuth2; credentials managed via `mailTechUser` / `mailTechPass` JNDI; intentional architecture choice — out of scope |

---

## 📋 Migration Log

| Date | Endpoint(s) | Registration Used | PR / Commit |
|---|---|---|---|
| 2026-06-02 | `pomUrl` → `https://corso.api-np.swissre.com/pom-api/train1/policies/` (re-migration) | `pom-default` | `MailHelperBO.sendMailNotification()` switched from `restService.getCall(pomUrl + policyNum + "?format=LEGACY")` (Basic Auth) to `restService.getCallOAuth2(pomUrl + policyNum + "?format=LEGACY", OAuth2ClientConfig.POM_DEFAULT)`. `pomUrl` JNDI updated to Corso API gateway: train1 `corso.api-np.swissre.com/pom-api/train1/policies/`, train2 `corso.api-np.swissre.com/pom-api/train2/policies/`, prod `corso.api.swissre.com/pom-api/policies/`. No new sibling method needed — `getCallOAuth2()` already existed. |
| 2026-04-22 | **REVERT** `pomUrl` → reverted from `https://corso.api-np.swissre.com/pom-api/train1/policies/` back to `https://pom-train1.swissreapps-np.com/polmgr/api/policies/` (and equivalents for train2 / prod) | `pom-default` (reverted) | Surgical revert of POM call site only: `MailHelperBO.sendMailNotification()` switched back to `restService.getCall(pomUrl + policyNum + "?format=LEGACY")` (Basic Auth); `pomUrl` JNDI values restored to legacy hosts across all 3 envs. **Not reverted:** `getCallOAuth2()` sibling in `RestService`, `pomDefaultOauth2Scope` / `swisscomOAuthClient` / `swisscomOAuthSecret` JNDI entries, `OAuth2ClientConfig.POM_DEFAULT` constant. Reason: POM not ready on production side. |
| 2026-04-03 | `pomUrl` → `https://corso.api-np.swissre.com/pom-api/train1/policies/{policyNum}?format=LEGACY` · `dealUrl` → `https://uwbench-train1.swissreapps-np.com/deal/api/Deals/{id}` (rows 1–3) | `pom-default` (row 1) · `uwb-default` (rows 2 & 3) | Added `getCallOAuth2(url, registrationId)` sibling in `RestService`; injected `Oauth2TokenService`; updated `MailHelperBO.sendMailNotification()` to use `POM_DEFAULT` and `setAttributes()` to use `UWB_DEFAULT`; legacy `getCall()` Basic-Auth method preserved unchanged; `pomUrl` JNDI updated to Corso API gateway: train1 `corso.api-np.swissre.com/pom-api/train1/policies/`, train2 `corso.api-np.swissre.com/pom-api/train2/policies/`, prod `corso.api.swissre.com/pom-api/policies/` |

---

## 📌 Key Reference

### Auth mechanism legend

| Symbol | Meaning |
|---|---|
| **New Infra OAuth2** | `Oauth2TokenService.getAccessToken(registrationId)` — Spring Security `AuthorizedClientServiceOAuth2AuthorizedClientManager` with in-memory token cache and auto-refresh |
| **SMTP AUTH** | `JavaMailSenderImpl` credentials (`mailTechUser` / `mailTechPass`) used to authenticate to the corporate SMTP relay |

### Registration IDs (`OAuth2ClientConfig`)

| Registration ID | Scope JNDI Key | Scope Value (train1 / NP) | Scope Value (prod) | Credential JNDI Keys |
|---|---|---|---|---|
| `uwb-default` | `uwbDefaultOauth2Scope` | `api://cf62061e-1d78-4a1a-8ef3-114e09464d68/.default` | `api://76457445-4c4c-4090-a269-c0f60e6f4626/.default` | `swisscomOAuthClient` / `swisscomOAuthSecret` |
| `pom-default` | `pomDefaultOauth2Scope` | `api://pom-train.swissre.com/.default` | `api://pom.swissre.com/.default` | `swisscomOAuthClient` / `swisscomOAuthSecret` |

### URL Values Across Environments

| JNDI Key | train1 | train2 | prod |
|---|---|---|---|
| `pomUrl` | `https://corso.api-np.swissre.com/pom-api/train1/policies/` | `https://corso.api-np.swissre.com/pom-api/train2/policies/` | `https://corso.api.swissre.com/pom-api/policies/` |
| `dealUrl` | `https://uwbench-train1.swissreapps-np.com/deal/` | `https://uwbench-train2.swissreapps-np.com/deal/` | `https://uwbench.swissreapps.com/deal/` |
| `dealAppUrl` | `https://onedesk-train1.ac-np.swissre.com/#/dealDetails?dealNum=` | `https://onedesk-train2.ac-np.swissre.com/#/dealDetails?dealNum=` | `https://onedesk.ac.swissre.com/#/dealDetails?dealNum=` |
| `oneDeskUrl` | `http://onedesk-train1.ac-np.swissre.com/#/taskDetails?taskNum=` | `http://onedesk-train2.ac-np.swissre.com/#/taskDetails?taskNum=` | `http://onedesk.ac.swissre.com/#/taskDetails?taskNum=` |

> **Note:** `dealAppUrl` and `oneDeskUrl` are used only to construct hyperlinks embedded in the body of outgoing emails (via `MailBO` / `MailConstants`). They are **not** used as outbound HTTP call targets — no `HttpGet`, `RestTemplate`, or `RestService` call is made to these URLs. They are therefore correctly absent from both the 🔴 and ✅ tables.

### Files Relevant to Migrations

| File | Role |
|---|---|
| `RestService.java` | **Primary outbound HTTP utility** — `getCallOAuth2(url, registrationId)` performs OAuth2 Bearer-token authenticated GET requests using `Oauth2TokenService`. Legacy `getCall()` (Basic Auth) and `POSTCall()` (LTPA) have been removed; all callers migrated. |
| `MailHelperBO.java` | **Primary caller of `RestService`** — calls `restService.getCallOAuth2(pomUrl + policyNum, POM_DEFAULT)` and `restService.getCallOAuth2(dealUrl + "api/Deals/" + dealNum, UWB_DEFAULT)`. |
| `MailBO.java` | Sends emails via `JavaMailSender`; no outbound HTTP calls; consumes `dealAppUrl` as a string literal for email body only. |
| `OAuth2ClientConfig.java` | Registration definitions — read-only during migrations |
| `Oauth2TokenService.java` | Token vending service — read-only during migrations |
| `RestApplication.java` | JNDI bean wiring — contains `pomUrl`, `dealUrl`, `dealAppUrl`, `swisscomOAuthClient`, `swisscomOAuthSecret`, `uwbDefaultOauth2Scope`, `pomDefaultOauth2Scope` |
| `wlp/train1/server.xml` | JNDI values for train1 environment |
| `wlp/train2/server.xml` | JNDI values for train2 environment |
| `wlp/prod/server.xml` | JNDI values for production |

### Auth Mechanism Legend

| Symbol | Meaning |
|---|---|
| **New Infra OAuth2** | `Oauth2TokenService.getAccessToken(registrationId)` — Spring Security `AuthorizedClientServiceOAuth2AuthorizedClientManager` with in-memory token cache and auto-refresh |
| **SMTP AUTH** | `JavaMailSenderImpl` credentials (`mailTechUser` / `mailTechPass`) used to authenticate to the corporate SMTP relay |

