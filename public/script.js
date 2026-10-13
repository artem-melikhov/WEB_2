const canvas = document.getElementById("plot");
const ctx = canvas.getContext("2d");
const rInput = document.getElementById("r");

const SIZE = canvas.width;
const ORIGIN = SIZE / 2;
const WORLD = 5.5;
const SCALE = (SIZE / 2 - 24) / WORLD;

function currentR() {
  return Number(rInput.value);
}

function toCanvas(x, y) {
  return {
    px: ORIGIN + x * SCALE,
    py: ORIGIN - y * SCALE,
  };
}

function draw() {
  ctx.clearRect(0, 0, SIZE, SIZE);
  drawRegion();
  drawAxes();
  results.forEach(function (row) {
    if (row.r === currentR()) {
      drawPoint(row.x, row.y, row.hit);
    }
  });
}

function drawRegion() {
  const r = currentR();
  const half = r * SCALE / 2;

  ctx.save();
  ctx.translate(ORIGIN, ORIGIN);
  ctx.scale(1, -1);

  ctx.fillStyle = "rgba(70, 130, 180, 0.55)";
  ctx.strokeStyle = "rgba(40, 80, 120, 0.9)";
  ctx.lineWidth = 1;

  ctx.beginPath();
  ctx.rect(-r * SCALE, -half, r * SCALE, half);
  ctx.fill();
  ctx.stroke();

  ctx.beginPath();
  ctx.moveTo(-r * SCALE, 0);
  ctx.lineTo(0, 0);
  ctx.lineTo(0, half);
  ctx.closePath();
  ctx.fill();
  ctx.stroke();

  ctx.beginPath();
  ctx.moveTo(0, 0);
  ctx.arc(0, 0, half, 0, Math.PI / 2);
  ctx.closePath();
  ctx.fill();
  ctx.stroke();

  ctx.restore();
}

function drawAxes() {
  const r = currentR();

  ctx.strokeStyle = "#2c2c2c";
  ctx.fillStyle = "#2c2c2c";
  ctx.lineWidth = 1.5;
  ctx.font = "12px Arial, sans-serif";

  ctx.beginPath();
  ctx.moveTo(16, ORIGIN);
  ctx.lineTo(SIZE - 16, ORIGIN);
  ctx.moveTo(ORIGIN, SIZE - 16);
  ctx.lineTo(ORIGIN, 16);
  ctx.stroke();

  drawArrow(SIZE - 16, ORIGIN, 0);
  drawArrow(ORIGIN, 16, -Math.PI / 2);

  ctx.textAlign = "center";
  ctx.textBaseline = "top";
  ctx.fillText("x", SIZE - 14, ORIGIN + 8);

  ctx.textAlign = "left";
  ctx.textBaseline = "bottom";
  ctx.fillText("y", ORIGIN + 8, 16);

  const ticks = [
    { value: -r, label: (-r).toString() },
    { value: -r / 2, label: (-r / 2).toString() },
    { value: r / 2, label: (r / 2).toString() },
    { value: r, label: r.toString() },
  ];

  ctx.lineWidth = 1;
  ticks.forEach(function (tick) {
    const xTick = toCanvas(tick.value, 0);
    const yTick = toCanvas(0, tick.value);

    ctx.beginPath();
    ctx.moveTo(xTick.px, ORIGIN - 5);
    ctx.lineTo(xTick.px, ORIGIN + 5);
    ctx.stroke();

    ctx.textAlign = "center";
    ctx.textBaseline = "top";
    ctx.fillText(tick.label, xTick.px, ORIGIN + 8);

    ctx.beginPath();
    ctx.moveTo(ORIGIN - 5, yTick.py);
    ctx.lineTo(ORIGIN + 5, yTick.py);
    ctx.stroke();

    ctx.textAlign = "left";
    ctx.textBaseline = "middle";
    ctx.fillText(tick.label, ORIGIN + 8, yTick.py);
  });
}

function drawArrow(x, y, angle) {
  const length = 10;
  ctx.beginPath();
  ctx.moveTo(x, y);
  ctx.lineTo(
    x - length * Math.cos(angle - 0.4),
    y - length * Math.sin(angle - 0.4)
  );
  ctx.moveTo(x, y);
  ctx.lineTo(
    x - length * Math.cos(angle + 0.4),
    y - length * Math.sin(angle + 0.4)
  );
  ctx.stroke();
}

function drawPoint(x, y, hit) {
  const point = toCanvas(x, y);
  ctx.beginPath();
  ctx.fillStyle = hit ? "#1b7a3d" : "#a31d1d";
  ctx.arc(point.px, point.py, 4, 0, Math.PI * 2);
  ctx.fill();
  ctx.strokeStyle = "#2c2c2c";
  ctx.lineWidth = 1;
  ctx.stroke();
}

function parseDecimal(raw) {
  const text = String(raw).trim().replace(",", ".");
  if (!/^[-+]?(?:\d+(?:\.\d+)?|\.\d+)$/.test(text)) {
    return { ok: false };
  }
  const value = Number(text);
  if (!Number.isFinite(value)) {
    return { ok: false };
  }
  return { ok: true, value: value };
}

function validate(xValues, yRaw, rRaw) {
  const errors = [];
  let x;
  let y;
  let r;

  if (xValues.length !== 1) {
    errors.push("Выберите ровно одно значение X.");
  } else {
    x = Number(xValues[0]);
  }

  const parsedY = parseDecimal(yRaw);
  if (!parsedY.ok) {
    errors.push("Y должен быть числом.");
  } else if (parsedY.value <= -5 || parsedY.value >= 5) {
    errors.push("Y должен быть числом от -5 до 5, не включая границы.");
  } else {
    y = parsedY.value;
  }

  if (!["1", "1.5", "2", "2.5", "3"].includes(rRaw)) {
    errors.push("Выберите значение R из списка.");
  } else {
    r = Number(rRaw);
  }

  return { ok: errors.length === 0, errors: errors, x: x, y: y, r: r };
}

const STORAGE_KEY = "lab2-results";

function loadResults() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    const data = raw ? JSON.parse(raw) : [];
    return data;
  } catch (error) {
    return [];
  }
}

function saveResults(rows) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(rows));
  } catch (error) {
    errorBox.textContent = "Не удалось сохранить историю в браузере.";
  }
}

function formatTime(iso) {
  return new Date(iso).toLocaleString("ru-RU");
}

function renderResults(rows) {
  const tbody = document.querySelector("#results tbody");
  tbody.replaceChildren();
  rows.forEach(function (row) {
    const tr = document.createElement("tr");
    const cells = [
      row.x,
      row.y,
      row.r,
      row.hit ? "да" : "нет",
      formatTime(row.checkedAt),
      row.durationMs,
    ];
    cells.forEach(function (text) {
      const td = document.createElement("td");
      td.textContent = text;
      tr.appendChild(td);
    });
    tbody.appendChild(tr);
  });
}

const form = document.getElementById("point-form");
const errorBox = document.getElementById("form-error");
const submitButton = document.getElementById("submit-point");
const results = loadResults();

form.addEventListener("submit", async function (event) {
  event.preventDefault();

  const selectedX = Array.from(document.querySelectorAll("input[name='x']:checked")).map(function (input) { return input.value; });
  const parsed = validate(selectedX, document.getElementById("y").value, rInput.value);

  if (!parsed.ok) {
    errorBox.textContent = parsed.errors.join(" ");
    return;
  }

  errorBox.textContent = "";
  submitButton.disabled = true;

  try {
    const params = new URLSearchParams({ x: parsed.x, y: parsed.y, r: parsed.r });
    const response = await fetch("api/check?" + params.toString());
    const text = await response.text();
    let data;
    try {
      data = JSON.parse(text);
    } catch (error) {
      throw new Error("Сервер вернул ошибку " + response.status + ".");
    }
    if (!response.ok) {
      throw new Error(data.error || "Ошибка сервера.");
    }

    results.unshift(data);
    saveResults(results);
    renderResults(results);
    draw();
  } catch (error) {
    errorBox.textContent = error.message || "Не удалось связаться с сервером.";
  } finally {
    submitButton.disabled = false;
  }
});

document.getElementById("clear-results").addEventListener("click", function () {
  results.length = 0;
  saveResults(results);
  renderResults(results);
  draw();
});

rInput.addEventListener("change", draw);
renderResults(results);
draw();
