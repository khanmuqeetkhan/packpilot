const generateForm = document.getElementById("generate-form");
const resultBox = document.getElementById("result");
const resultTitle = document.getElementById("result-title");
const resultItems = document.getElementById("result-items");
const saveButton = document.getElementById("save-button");
const saveTripName = document.getElementById("save-trip-name");
const saveStatus = document.getElementById("save-status");
const tripList = document.getElementById("trip-list");
const refreshButton = document.getElementById("refresh-button");
const searchBox = document.getElementById("search-box");

let lastDraft = null;

generateForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const activities = Array.from(
    document.querySelectorAll("#generate-panel fieldset input[type=checkbox]:checked")
  ).map((cb) => cb.value);

  const body = {
    climate: document.getElementById("climate").value,
    tripLengthDays: Number(document.getElementById("trip-length").value),
    adults: Number(document.getElementById("adults").value),
    children: Number(document.getElementById("children").value),
    toddlers: Number(document.getElementById("toddlers").value),
    activities,
  };

  saveStatus.textContent = "";
  const response = await fetch("/packing-lists/generate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    alert("Could not generate a checklist. Is packing-generator-service reachable?");
    return;
  }
  lastDraft = await response.json();
  renderDraft(lastDraft);
});

function renderDraft(draft) {
  resultBox.classList.remove("hidden");
  const travelerBits = [];
  if (draft.adults) travelerBits.push(`${draft.adults} adult(s)`);
  if (draft.children) travelerBits.push(`${draft.children} child(ren)`);
  if (draft.toddlers) travelerBits.push(`${draft.toddlers} toddler(s)`);
  resultTitle.textContent =
    `${draft.climate} · ${draft.tripLengthDays} day(s) · ${travelerBits.join(", ")}` +
    (draft.activities.length ? ` · ${draft.activities.join(", ")}` : "");

  const byCategory = {};
  draft.items.forEach((item) => {
    byCategory[item.category] = byCategory[item.category] || [];
    byCategory[item.category].push(item);
  });

  resultItems.innerHTML = "";
  Object.keys(byCategory)
    .sort()
    .forEach((category) => {
      const block = document.createElement("div");
      block.className = "item-category";
      const heading = document.createElement("h4");
      heading.textContent = category;
      block.appendChild(heading);
      byCategory[category].forEach((item) => {
        const row = document.createElement("div");
        row.className = "item-row";
        const qty = document.createElement("span");
        qty.className = "qty";
        qty.textContent = `x${item.quantity}`;
        const name = document.createElement("span");
        name.textContent = item.name;
        row.appendChild(qty);
        row.appendChild(name);
        if (item.note) {
          const note = document.createElement("span");
          note.className = "note";
          note.textContent = `(${item.note})`;
          row.appendChild(note);
        }
        block.appendChild(row);
      });
      resultItems.appendChild(block);
    });

  saveTripName.value = "";
}

saveButton.addEventListener("click", async () => {
  if (!lastDraft) return;

  const body = {
    tripName: saveTripName.value || null,
    climate: lastDraft.climate,
    tripLengthDays: lastDraft.tripLengthDays,
    adults: lastDraft.adults,
    children: lastDraft.children,
    toddlers: lastDraft.toddlers,
    activities: lastDraft.activities,
    items: lastDraft.items,
  };

  const response = await fetch("/packing-lists", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

  if (response.ok) {
    saveStatus.textContent = "Saved!";
    loadTrips();
  } else {
    saveStatus.textContent = "Could not save.";
  }
});

async function loadTrips() {
  const params = new URLSearchParams();
  if (searchBox.value) params.set("search", searchBox.value);

  const response = await fetch(`/packing-lists?${params.toString()}`);
  if (!response.ok) {
    tripList.innerHTML = "<p>Could not load saved trips.</p>";
    return;
  }
  const trips = await response.json();
  renderTrips(trips);
}

function renderTrips(trips) {
  tripList.innerHTML = "";
  if (trips.length === 0) {
    tripList.innerHTML = "<p>No saved trips yet. Generate a checklist above.</p>";
    return;
  }
  trips
    .sort((a, b) => (b.createdAt || "").localeCompare(a.createdAt || ""))
    .forEach((trip) => {
      const card = document.createElement("div");
      card.className = "trip-card";

      const header = document.createElement("div");
      header.className = "trip-card-header";

      const headerText = document.createElement("div");
      const title = document.createElement("h4");
      title.textContent = trip.tripName;
      const meta = document.createElement("p");
      const checkedCount = trip.items.filter((i) => i.checked).length;
      meta.textContent = `${trip.climate} · ${trip.tripLengthDays} day(s) · ${checkedCount}/${trip.items.length} packed`;
      headerText.appendChild(title);
      headerText.appendChild(meta);

      const actions = document.createElement("div");
      actions.className = "trip-card-actions";
      const deleteButton = document.createElement("button");
      deleteButton.textContent = "Delete";
      deleteButton.className = "danger";
      deleteButton.addEventListener("click", (e) => {
        e.stopPropagation();
        removeTrip(trip.id);
      });
      actions.appendChild(deleteButton);

      header.appendChild(headerText);
      header.appendChild(actions);

      const body = document.createElement("div");
      body.className = "trip-card-body hidden";
      trip.items.forEach((item) => {
        const row = document.createElement("div");
        row.className = "checklist-row" + (item.checked ? " checked" : "");
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.checked = item.checked;
        checkbox.addEventListener("change", () => toggleItem(trip.id, item.id, checkbox.checked));
        const name = document.createElement("span");
        name.className = "checklist-name";
        name.textContent = `${item.name} (x${item.quantity})`;
        const removeBtn = document.createElement("button");
        removeBtn.textContent = "Remove";
        removeBtn.addEventListener("click", () => removeItem(trip.id, item.id));
        row.appendChild(checkbox);
        row.appendChild(name);
        row.appendChild(removeBtn);
        body.appendChild(row);
      });

      header.addEventListener("click", () => body.classList.toggle("hidden"));

      card.appendChild(header);
      card.appendChild(body);
      tripList.appendChild(card);
    });
}

async function toggleItem(tripId, itemId, checked) {
  await fetch(`/packing-lists/${tripId}/items/${itemId}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ checked }),
  });
  loadTrips();
}

async function removeItem(tripId, itemId) {
  await fetch(`/packing-lists/${tripId}/items/${itemId}`, { method: "DELETE" });
  loadTrips();
}

async function removeTrip(tripId) {
  await fetch(`/packing-lists/${tripId}`, { method: "DELETE" });
  loadTrips();
}

refreshButton.addEventListener("click", loadTrips);
searchBox.addEventListener("input", () => {
  clearTimeout(window.__searchDebounce);
  window.__searchDebounce = setTimeout(loadTrips, 300);
});

loadTrips();
