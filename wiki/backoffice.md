# Backoffice

The backoffice is a small web UI running on port `9996`. It lets you inspect
and manage conversations, scheduled tasks, user profiles and uploaded files.

## Index page

Open `http://yourAddress:9996/backoffice` to access the index. The page shows:

* A form to send anonymous prompts using the `/api/prompt` endpoint.
* A table with the known conversation ids. Each id links to a conversation page.
* A table listing all scheduled tasks stored in the database.

## Conversation page

Clicking a conversation id opens `/backoffice/conversations/{chatId}`. This page
provides several sections:

* **Messages** – paginated list of messages with buttons to edit or delete each
  entry and a form to append new messages.
* **Profile** – textarea to view or update the profile associated with the
  conversation.
* **Files** – list of uploaded files with download, delete and rename actions,
  plus an upload form.
* **Tasks** – tasks scheduled for the chat with a delete button.

Navigation links allow moving between pages of messages and returning to the
index.

All forms are handled by `backoffice.js` which submits requests to the `/api`
endpoints and refreshes the content dynamically.
