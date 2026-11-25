# it-docs User Guide

> 中文版见 [使用说明.md](使用说明.md)
>
> This guide is for **people who look things up and write records every day**. For deployment, LDAP setup, backup and restore, see [DEPLOYMENT.en.md](DEPLOYMENT.en.md).

## 1. What it is for

IT support knowledge used to live in chat threads, personal notes and a static page — hard to search, easy to lose.
This platform keeps **Q&A records** organised by business system, so that *finding the error message is enough to follow the fix*.
It also holds the business and technical documents that need long-term maintenance (requirements, interface specs, training manuals).

## 2. Opening and signing in

| Item | Notes |
|---|---|
| URL | Provided by your administrator, e.g. `http://<server-ip>/` |
| Domain account | Your AD account (`sAMAccountName`, e.g. `wang.jz`) — **do not** prefix it with `CORP\` |
| Local account | Created by an administrator, e.g. `admin`; ask an administrator to reset the password |
| First sign-in | A domain account is provisioned automatically with the **Viewer** role; ask an administrator to raise it |

The login page has a brand panel on the left and the form on the right. Switching between "Domain account" and "Local account" only changes the input hints — the backend detects the account type.

> Signing in as `admin / admin@123` the first time forces a password change.

## 3. What you can see without signing in (public portal)

The home page opens without login, but **only Q&A records are listed**; business and technical documents require login.

- Two filter panels on the left: **business system** and **issue category** (categories narrow to the selected system)
- The search box matches titles and full text
- Click any row to open the detail page and copy the link to a colleague
- On a phone the portal is read-only and responsive

## 4. The console (after signing in)

Once signed in, "Console" and "Personal area" appear in the top navigation.

### 4.1 Console home

- Four stat cards: Q&A / business documents / technical documents / personal drafts
- Business system cards below; each opens three tabs — Q&A, business docs, technical docs
- Left menu: Console home / All Q&A / All business docs / All technical docs / Personal area

### 4.2 List view

- Filter by business system or search by keyword
- Roles with write access get **New**, **Export CSV** and **Bulk import CSV** (a template is available on the import dialog)
- Row actions: Edit / Versions / Preview / Restore (after being unpublished) / Unpublish / Delete

### 4.3 Creating content: three types

| Type | Use it for | Main fields |
|---|---|---|
| Q&A `qa` | How to fix an error | Title, system, category, tags, **error text, cause, steps, notes**, screenshots |
| Business doc `req` | Requirements, processes, rules | Title, system, tags, Markdown body |
| Technical doc `tech` | Interfaces, deployment, training manuals | Title, system, tags, Markdown body |

Before saving:

- **Type cannot be changed** once saved — create a new document if you picked the wrong one
- **Knowledge space** decides who can read it (see section 6)
- Enter one step per line; the body supports Markdown (headings, lists, tables, code blocks, images)
- Type a tag and press Enter, e.g. `high-frequency`, `training-manual`

### 4.4 Recommended order for a Q&A record

1. Put the **symptom** in the title, e.g. "SVS login says the account is locked"
2. Pick the right business system and category so it can be filtered later
3. Paste the **error text** verbatim (also type out text that appears only in screenshots — otherwise it cannot be searched)
4. State the **cause** in one sentence
5. Write the **steps** one per line so a colleague can follow them end to end
6. Attach screenshots where useful; each can have a caption

## 5. Personal area: draft first, publish later

- Drafts in the personal area are visible **only to you**; even administrators cannot read them
- Jot a note when an issue appears, then click **Publish** and pick the target public space
- Accounts with no writable public space can still draft; an administrator can publish on their behalf

## 6. How permissions work

### Roles

| Role | Capabilities |
|---|---|
| Viewer `VIEWER` | Read public content and content in spaces shared with them; the default for a first-time domain login |
| Editor `EDITOR` | Create, edit, unpublish, restore and delete inside authorised spaces |
| Admin `ADMIN` | Everything above plus members, spaces and permissions, base data, system settings |

### Space visibility

| Visibility | Meaning |
|---|---|
| Public `PUBLIC` | Readable without login (currently the Q&A space only) |
| Internal `INTERNAL` | Readable after login (business and technical documents) |
| Restricted `RESTRICTED` | Only subjects on the space's access list |

### Per-document access

"Access scope" in the editor's top right grants access to a single document: user names, or AD groups (group name or full DN).
Domain-group authorisation needs the bind account configured by an administrator, and only matches direct memberships — nested groups are not resolved.

## 7. Versions, unpublishing and deletion

- **Versions**: every save keeps a version; click "Versions" on a row to browse history and "Restore" to write an older version back (title, summary, system, category, owner role, tags and body are all restored)
- **Unpublish / Restore**: an unpublished document disappears from the portal but stays visible in the console and can be restored at any time
- **Delete**: irreversible — only delete content you are sure is no longer needed

## 8. FAQ

| Symptom | What to do |
|---|---|
| "Wrong account or password" | For a domain account, make sure the name has no `CORP\` prefix; for a local account, check with an administrator whether it is disabled |
| Business documents invisible after login | The account is Viewer, or its `sys_scope` does not include that system — ask an administrator |
| A record cannot be found by search | Check it is not unpublished; search covers published content only; paste error text instead of screenshot-only |
| Save button missing or disabled | No write access in the current space — ask for authorisation, or save to a personal draft first |
| Images do not render | Attachments travel with the document; re-upload images that were copied as external links from the old system |
| Need the history of a document | Console list → "Versions" on that row |
