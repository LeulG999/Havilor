# Flow F30: Account management (the manager acts as admin)

**Goal:** a manager adds people, turns accounts off and on and changes roles; a new person gets a temporary
password once and must choose their own before using Havilor; everyone can change their password.
**New idea:** **one-time secrets** (generated with `SecureRandom`, shown once, stored only as a BCrypt hash) and a
**security filter driven by a token claim** (no database lookup on every request).

**Built on:** `main` · **Status:** done, 190 backend tests pass (3 new), `npm run build` passes, checked against a running
backend, pushed

## How it works

```mermaid
sequenceDiagram
    participant M as Manager (Team page)
    participant U as UserController / AccountService
    participant A as AuthService
    participant F as PasswordChangeFilter
    participant P as New person
    M->>U: POST /api/v1/users {name, email, role}  (MANAGER only)
    U->>U: temporary password: 20 chars, SecureRandom; store BCrypt hash; mustChangePassword = true; audit USER_CREATED
    U-->>M: {user, temporaryPassword}  ← the only time it is shown
    P->>A: POST /auth/login (temporary password) → token with claim mustChangePassword=true
    P->>F: GET /api/v1/alerts → 403 "Set a new password first"  (only /users/me and /users/me/password pass)
    P->>A: POST /api/v1/users/me/password {currentPassword, newPassword ≥ 12}
    A-->>P: NEW token without the claim → Havilor opens
    M->>U: PATCH /api/v1/users/{id} {active, role}  → audit USER_DEACTIVATED / USER_REACTIVATED / ROLE_CHANGED
```

## Files

### Backend

| File | What it does |
|---|---|
| `db/migration/V19__user_must_change_password.sql` (new) | `app_user.must_change_password`, false for existing users. |
| `user/domain/User.java` | `mustChangePassword`; `withTemporaryPassword(...)`, `changePassword`, `changeActive`, `changeRole`. |
| `user/service/AccountService.java` (new) | List everyone; create (unique email → 409, temporary password, hash only); update (deactivating yourself → 409); audit entries (entity type `USER`). |
| `user/service/AuthService.java` | `tokenFor(user)` adds the `mustChangePassword` claim; `changePassword` (wrong current → 400) returns a new token. |
| `user/api/UserController.java` | `GET /users` (no role = everybody, for the Team page), `POST /users`, `PATCH /users/{id}`, `POST /users/me/password`. |
| `user/api/request/*`, `user/api/response/CreatedUserResponse.java`, `UserResponse`, `LoginResponse` | The new shapes; `mustChangePassword` in `/users/me` and the login answer. |
| `common/security/PasswordChangeFilter.java` (new), `SecurityConfiguration.java` | The filter after the token check; `POST /users` and `PATCH /users/*` MANAGER only. |
| `common/error/PasswordChangeRequiredException.java` (new), `GlobalExceptionHandler.java` | 403 "Set a new password first". |
| `audit/service/AuditService.java` | `USER` entity type. |
| `test/.../user/AccountIntegrationTest.java` (new) | Temporary password once and only hashed; first login → 403 everywhere but /me and /me/password; wrong/short password → 400; new token works; old password gone; deactivate → no login, reactivate → login; role change; audit actions; self-deactivate 409; duplicate email 409; bad input 400; unknown 404; analysts 403. |

### Front end

| File | What it does |
|---|---|
| `src/pages/TeamPage.tsx` (new) | People (avatar, name, email, role select, account status: Active / Must set password / Turned off), Deactivate / Reactivate (not for yourself). |
| `src/components/AddPersonButton.tsx` (new) | "Add person" modal; then the temporary password in a mono box with Copy and "You won't see this password again". Closing forgets it. |
| `src/pages/SetPasswordPage.tsx` (new) | The first-login screen in the login page's design. |
| `src/components/ChangePasswordForm.tsx` (new) | Current + new + repeat; saves the new token. Used on the first-login screen and in the avatar menu. |
| `src/components/LoggedInLayout.tsx` | Loads `/users/me` once; `mustChangePassword` → only the first-login screen. |
| `src/components/Sidebar.tsx` | Gets the user from the layout; "Team" rail link for managers; "Change password" in the avatar menu. |
| `src/components/icons.tsx`, `TopBar.tsx`, `App.tsx`, `api/types.ts`, `index.css` | People and key icons, "Manager / Team", the route, the types, the styles. |

## Checked against a running backend

Manager created "Sam Newcomer" → the answer had the user with `mustChangePassword: true` and a temporary password;
logging in with it gave a token that got 403 on `/alerts`; the app showed only "Set your password". The Team page
listed the three people with roles and statuses. Screenshots compared with the design.

## Decisions

- **The claim is in the token**, so the filter needs no database lookup per request; changing the password returns a
  new token without it. A token can't outlive a deactivation for more than its lifetime (8 h); login already refuses
  deactivated accounts.
- **Temporary password: 20 characters from a 57-character alphabet without look-alikes** (0/O, 1/l/I), from
  `SecureRandom`.
- **Only "deactivate yourself" is refused** for a manager, as asked; changing your own role is allowed (worth a rule
  later if the team could end up without a manager).
- **Password changes are not audited** (not asked); account changes are.
- **No delete:** accounts are turned off, so their history (actions, notes, audit) keeps a valid person.

## How to verify

As the manager: rail → Team → "+ Add person" → name, email, role → the temporary password with Copy and the warning.
Log out, log in as that person with it → only "Set your password" → set one (12+) → Havilor opens. Back as the
manager: Deactivate → that person can't log in; Reactivate; change a role. Avatar → "Change password" works for
everyone. As an analyst, `/team` shows the backend's 403 message.

## New concepts introduced

- **`SecureRandom`** for secrets; **show once, store a hash**.
- **A servlet filter in the Spring Security chain** that reads a JWT claim.
- **Re-issuing a token** after a state change.
- **Gating the whole UI** on one flag from `/users/me`.
