
document.addEventListener('DOMContentLoaded', () => {
  const autoResize = (textarea) => {
    textarea.style.height = 'auto';
    textarea.style.height = textarea.scrollHeight + 'px';
  };

  const reloadMessages = async () => {
    const table = document.getElementById('messagesTable');
    if (!table) return;
    const chatId = table.dataset.chatid;
    const page = table.dataset.page;
    const resp = await fetch(`/api/conversations/${chatId}/messages?page=${page}`);
    if (!resp.ok) return;
    const messages = await resp.json();
    const tbody = table.querySelector('tbody');
    tbody.innerHTML = '';
    messages.forEach(m => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${m.role}</td>
        <td><textarea rows="2" name="content">${m.content}</textarea></td>
        <td>${m.timestamp}</td>
        <td>
          <form action="/api/conversations/${chatId}/messages/${m.id}" data-fetch data-update="messages" data-method="PUT">
            <button type="submit">Save</button>
          </form>
        </td>
        <td>
          <form action="/api/conversations/${chatId}/messages/${m.id}" data-fetch data-update="messages" data-method="DELETE">
            <button type="submit">Delete</button>
          </form>
        </td>`;
      tbody.appendChild(tr);
    });
    tbody.querySelectorAll('form[data-fetch]').forEach(sendWithFetch);
    tbody.querySelectorAll('textarea').forEach(t => {
      autoResize(t);
      t.addEventListener('input', () => autoResize(t));
    });
  };

  const sendWithFetch = form => {
    form.addEventListener('submit', async e => {
      e.preventDefault();
      const method = form.dataset.method || form.method || 'post';
      const resp = await fetch(form.action, {
        method: method.toUpperCase(),
        body: new FormData(form)
      });
      if (form.id === 'promptForm') {
        const text = await resp.text();
        document.getElementById('promptResult').textContent = text;
        form.reset();
      } else if (form.id === 'systemMessageForm') {
        const text = await resp.text();
        document.getElementById('systemMessageResult').textContent = text;
        form.reset();
      } else if (form.dataset.update === 'messages' && resp.ok) {
        await reloadMessages();
      } else if (resp.ok) {
        window.location.reload();
      } else {
        alert('Action failed');
      }
    });
  };

  document.querySelectorAll('form[data-fetch]').forEach(sendWithFetch);
  document.querySelectorAll('textarea').forEach(t => {
    autoResize(t);
    t.addEventListener('input', () => autoResize(t));
  });
});

