const width = document.querySelector("#width");
const zones = document.querySelector("#zones");
const board = document.querySelector(".board");

function showWidth() {
  board.style.setProperty("--preview-width", width.value + "px");
  board.querySelector(".width-label").textContent = width.value;
}
function showZones() {
  board.classList.toggle("show-zones", zones.checked);
}
width.addEventListener("change", showWidth);
zones.addEventListener("change", showZones);
showWidth();
showZones();

// Match visible ink height after accounting for stroke, keeping each shape's proportions.
document.querySelectorAll(".bar svg path").forEach(path => {
  const { x, y, width, height } = path.getBBox();
  const scale = 14.2 / height;
  const dx = 12 - (x + width / 2) * scale;
  const dy = 12 - (y + height / 2) * scale;
  path.setAttribute("transform", "translate(" + dx + " " + dy + ") scale(" + scale + ")");
});

document.querySelectorAll(".choice").forEach(choice => {
  const output = choice.querySelector("output");
  const date = choice.querySelector(".date");
  choice.querySelectorAll(".tab").forEach(tab => {
    tab.addEventListener("click", () => {
      choice.querySelectorAll(".tab").forEach(item => item.setAttribute("aria-selected", String(item === tab)));
      date.disabled = tab.dataset.module !== "tasks";
      date.setAttribute("aria-label", date.disabled ? "21 ноября, суббота" : "Вернуться к сегодня, 21 ноября");
      output.textContent = tab.getAttribute("aria-label");
    });
  });
  choice.querySelectorAll(".gap").forEach(gap => {
    gap.addEventListener("click", () => { output.textContent = gap.getAttribute("aria-label"); });
  });
  choice.querySelector(".add").addEventListener("click", () => { output.textContent = "Добавить"; });
  choice.querySelector(".refresh").addEventListener("click", () => { output.textContent = "Обновление"; });
  date.addEventListener("click", () => { output.textContent = "Вернуться к сегодня"; });
});
