# Telegram tools

Helper methods bound to a specific chat. They allow the AI to interact with the user and the upload folder.

## sendReminder
Schedule a reminder message on a given date.
Arguments: `message`, `dateTime` (format `yyyy.MM.dd HH:mm`).

## deleteReminder
Remove a scheduled reminder identified by its key.

## listReminders
List the reminders registered for the chat.

## sendMessage
Send a markdown message to the user.

## sendFile
Send a file from the upload folder with a caption.
Arguments: `fileName`, `caption`.

## listUploadedFiles
Return the list of files uploaded by the user.

## getFileFullPath
Return the absolute path of a file in the upload folder.

## deleteFile
Delete a file from the upload folder.

## renameFile
Rename a file inside the upload folder.
Arguments: `currentName`, `newName`.

## readFile
Read the contents of a text file stored in the upload folder.

## saveAsFile
Save text content as a file in the upload folder.
Arguments: `fileName`, `fileContents`.

## sendAsFile
Send text as a temporary file to the user.
Arguments: `fileName`, `fileContents`.
