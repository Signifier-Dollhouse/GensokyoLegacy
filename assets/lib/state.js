// View state and browser-stored preferences.

/**
 * Repository the page is published from. Used only to opportunistically refresh
 * the file index so new content shows up without a rebuild; if the request fails
 * the committed index is used as-is.
 */
export const REPO = { owner: "Signifier-Dollhouse", name: "GensokyoLegacy", ref: "gh-page" };

/** Content types, in tab order. `quest`/`daily` and `starters`/`dialog` each split
 *  one registry in two; see `app.js` for what the split is. Items are not a tab -
 *  they are a sidebar section of their own, since they belong to no character. */
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
  /** Which sidebar section is showing: a character's content, or the item guide. */
  section: "character",
  character: "all",
  /** The guide category being listed, or null for every item. */
  category: null,
  /**
   * Item groups the reader has folded away. The sidebar is redrawn on every selection,
   * and a `<details>` forgets it was closed when its element is rebuilt, so the state
   * is kept here - keyed by the section id, which is the same in every language.
   */
  collapsed: new Set(),
  tab: "quest",
  query: "",
  /** Dialog files are only fetched the first time the dialog tab is opened. */
  dialogsLoaded: false,
  /** Nor are the recipes and quest reward tables, behind the first item page. */
  sourcesLoaded: false,
};

export function setLanguage(locale) {
  state.lang = locale;
  write("lang", locale);
}

export function setTheme(theme) {
  state.theme = theme;
  write("theme", theme);
}
