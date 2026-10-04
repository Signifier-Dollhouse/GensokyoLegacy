// Minimal DOM helpers. Everything is built from real nodes and text, so datapack
// strings are never parsed as HTML.

/** Hyperscript. `text` and string children are inserted as text nodes. */
export function h(tag, props, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(props ?? {})) {
    if (value === null || value === undefined || value === false) continue;
    if (key === "class") node.className = value;
    else if (key === "text") node.textContent = value;
    else if (key === "dataset") Object.assign(node.dataset, value);
    else if (key.startsWith("on")) node.addEventListener(key.slice(2).toLowerCase(), value);
    else node.setAttribute(key, value === true ? "" : value);
  }
  return append(node, children);
}

/** Appends children, flattening arrays and skipping empty values. */
export function append(parent, children) {
  for (const child of children.flat(Infinity)) {
    if (child === null || child === undefined || child === false) continue;
    parent.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
  return parent;
}

/**
 * Removes all children and returns the node, or null when the page does not have that
 * element. The page is assembled from files that change independently of it, so a
 * browser holding a cached `index.html` can be running a newer module than its markup;
 * losing one list is better than losing the render.
 */
export function clear(node) {
  node?.replaceChildren();
  return node;
}

/** Appends to a node that may be absent, so a missing element is a no-op. */
export function fill(node, ...children) {
  if (node) append(node, children);
  return node;
}

/** The single page-level status line. */
export function setStatus(message, isError = false) {
  const node = document.querySelector("#status");
  node.classList.toggle("error", isError);
  node.textContent = message;
}

/** The load progress bar, defined in index.html. */
export function setProgress(done, total) {
  const track = document.querySelector("#loadbar");
  const bar = document.querySelector("#progress");
  if (!track || !bar) return;
  track.hidden = total === 0;
  if (!total) return;
  bar.style.width = `${Math.round((done / total) * 100)}%`;
  track.dataset.label = `${done} / ${total}`;
}
