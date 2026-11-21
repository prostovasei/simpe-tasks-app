document.querySelectorAll(".variant").forEach(variant => {
  const rows = [...variant.querySelectorAll(".week")];
  const title = variant.querySelector(".sunday-title span");
  const tasks = variant.querySelector(".sunday-tasks");
  const initial = rows.find(row => row.getAttribute("aria-pressed") === "true");
  for (const row of rows) {
    row.addEventListener("click", () => {
      for (const item of rows) item.setAttribute("aria-pressed", String(item === row));
      title.textContent = row.dataset.sunday;
      tasks.hidden = row !== initial;
    });
  }
});
