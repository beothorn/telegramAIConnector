
document.addEventListener('DOMContentLoaded', () => {
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
      } else if (resp.ok) {
        window.location.reload();
      } else {
        alert('Action failed');
      }
    });
  };

  document.querySelectorAll('form[data-fetch]').forEach(sendWithFetch);
});

