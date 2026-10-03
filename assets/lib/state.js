// View state and browser-stored preferences.

/**
 * Repository the page is published from. Used only to opportunistically refresh
 * the file index so new content shows up without a rebuild; if the request fails
 * the committed index is used as-is.
 */
export const REPO = { owner: "Signifier-Dollhouse", name: "GensokyoLegacy", ref: "gh-page" };

/** Content types, in tab order. `quest`/`daily` and `starters`/`dialog` each split
 *  one registry in two; see `app.js` for what the split is. */
export const TABS = ["quest", "daily", "trade", "starters", "dialog"];

function read(key, fallback) {
  try {
    return localStorage.getItem(`gl-${key}`) ?? fallback;
  } catch {
    return fallback; // private mode
  }
}

function write(key, value) {
  try {
    localStorage.setItem(`gl-${key}`, value);
  } catch {
    /* preference is simply not persisted */
  }
}

export const state = {
  lang: read("lang", (navigator.language || "en").toLowerCase().startsWith("zh") ? "zh_cn" : "en_us"),
  theme: read("theme", null),
  character: "all",
  tab: "quest",
  query: "",
  /** Dialog files are only fetched the first time the dialog tab is opened. */
  dialogsLoaded: false,
};

export function setLanguage(locale) {
  state.lang = locale;
  write("lang", locale);
}

export function setTheme(theme) {
  state.theme = theme;
  write("theme", theme);
}
