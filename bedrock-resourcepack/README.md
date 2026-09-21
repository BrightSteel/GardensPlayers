# Gardens Shop UI (Bedrock / Geyser)

Bedrock resource pack that re-skins Gardens' Geyser forms into MineVille-style menus, while leaving
every other Geyser form (e.g. `/rtp`, other plugins) untouched. Currently it themes:

- **`/shop`** (`BedrockShopMenu`) — a wide window with a paginated 9-column grid of dark item cards
  (price + icon) and a per-item green **Buy** sub-button.
- **`/warp`** (`BedrockWarpsMenu`) — a wide window with category tabs and a paginated 7×3 grid of
  **player-head tiles** (head icons loaded as image URLs from mc-heads.net, mirroring the Java menu's
  player-head items). Clicking a tile warps; a **Yours** toggle lists the viewer's own warps.
- **`/quests`** (`BedrockQuestsMenu`) — a wide window with a paginated 4×3 grid of the daily quest tiles
  (category icon on top, task name/progress below, full reward/status on hover; completed tasks use the
  `_completed` icon variant). Read-only: tiles are hover-only and don't close the form; Prev/Next page
  through them, a **Leaderboard** button opens the ranked view, and a footer shows completed count,
  reset timer, and page.
- **`/quests` leaderboard** (`BedrockQuestLeaderboardMenu`) — a wide window with a 3×3 grid of the top
  quest completers as **player-head tiles** (rank + name + quests; heads from mc-heads.net). Read-only;
  reached from the quests menu's Leaderboard button, returns via Back.
- **`/rtp`** (`BedrockRtpMenu`) — a compact window with two text tabs (**Build** / **Resources**), each
  showing a row of three dimension tiles (emblem icon + world name below). Emblems under
  `textures/gardens/rtp/` are the corner badges cut out of the source card art. Tapping a tile runs
  `/rtp <world>`.
- **enchanting table** (`BedrockEnchantingMenu`, in the **gardens-enchants** plugin) — a compact window
  with a vertical list of enchant-option rows (enchant name + XP/money cost), styled with the existing
  shop card art. Opened when a Bedrock player right-clicks an enchanting table **while holding an
  unenchanted book**; picking a row closes the form and turns the held book into that enchanted book.
  Note this is the one themed form sent by a *different* plugin — it only needs the token + frame here.
- **enchanting books** (`BedrockEnchantingBooksMenu`, in **gardens-enchants**) — a wide window with a
  paged 5×3 grid of read-only book tiles (book icon + name, full item lore on hover), reached from the
  enchant menu's **View Enchanting Books** button. Prev/Next page through them; Back returns to the
  enchant menu.

## Flow

`category grid → item grid → (main tile → lore screen | Buy strip → buy window) → Yes/No confirm`

- **Item grid** — 9×3 cells/page. Each cell: buy price on top (`$` acorns / `@` gems), item icon,
  and a green **Buy** strip. Pagination via Prev/Next.
- **Main tile click** → lore screen (item lore + prices, Buy / Back).
- **Buy strip click** → buy window (quantity) → confirm modal.

## How it works (plugin ↔ pack contract)

Form titles carry a hidden private-use glyph, rendered invisibly by the blank `font/glyph_E1.png`:

| Token | Codepoint | Used on | Pack layout |
|-------|-----------|---------|-------------|
| `GRID_TOKEN` | `U+E111` | shop category + item grids | wide window + custom item grid |
| `CAT_TOKEN` | `U+E112` | shop category grid | wide window + category grid |
| `SHOP_TOKEN` | `U+E110` | lore / detail / confirm | normal vanilla form |
| `WARP_TOKEN` | `U+E113` | `/warp` list | wide window + head-tile grid |
| `QUEST_TOKEN` | `U+E114` | `/quests` list | wide window + quest-tile grid |
| `QUESTLB_TOKEN` | `U+E115` | `/quests` leaderboard | wide window + head-tile grid |
| `RTP_TOKEN` | `U+E116` | `/rtp` dimension picker | compact window + 2 tabs + 3 card tiles |
| `ENCHANT_TOKEN` | `U+E117` | enchanting-table book menu | compact window + vertical list of option rows |
| `EBOOKS_TOKEN` | `U+E118` | enchanting-books catalogue | wide window + paged 5×3 book-tile grid |

`ui/server_form.json` is a **full override** of the vanilla file (a recent copy from
[Mojang/bedrock-samples](https://github.com/Mojang/bedrock-samples/blob/main/resource_pack/ui/server_form.json)
with our controls added). It overrides `long_form` into title-gated frames: a normal-width vanilla
frame (any form whose title carries none of the tokens above) plus the wide `gardens_shop_form`,
`gardens_category_form`, `gardens_warps_form`, `gardens_quests_form`, `gardens_qlb_form`, and the
compact `gardens_rtp_form`. **When adding a new wide frame, gate it on a fresh `U+E11x` token _and_ add
that token to the vanilla frame's exclusion binding**, or the vanilla frame renders behind it.

### RTP picker (`gardens_rtp_form` / `gardens_rtp_panel`)

One `SimpleForm` per view; `BedrockRtpMenu` sends buttons in a single fixed block:

- `0..2` — the active tab's three dimension tiles (emblem icon + world-name label; `FormImage.Type.PATH`
  from `textures/gardens/rtp/`; click = `/rtp <world>`). Padded with empty buttons if a tab has fewer.
- `3` Build tab, `4` Resources tab (text buttons, active one highlighted green; switch tabs in place).

Tiles reuse the warps view-cell layout: a `cell_bg` card (which hides the vanilla button background) with
the emblem on top and the name below. The `card_row`/`tab_row` stack panels fill the full body width
(3×84 and 2×126 px) so they self-center. Emblems are the corner badges cut out of the original card art.

### Enchanting table (`gardens_enchant_form` / `gardens_enchant_panel`)

One `SimpleForm`, sent by **gardens-enchants**' `BedrockEnchantingMenu`. The plugin sends exactly 3
buttons (`ENCHANT_GENERATE_COUNT`) so they map to the panel's three fixed rows; unused rows are sent as
empty-text buttons and hidden (each row's `#visible` is gated on `#form_button_text` being non-empty).
Each row is a wide `button_bg` card whose `gardens_enchant_cell_content` shows the enchant's **book
icon** on the left (bound to `#form_button_texture`; the plugin sends `FormImage.Type.PATH` →
`textures/items/books/<enchant_id>`) next to the two-line button text (coloured enchant name on top,
cost line below); clicking fires `button.form_button_click`, and the plugin applies that enchant to the
held book. Reuses the shop textures (`title_bar`, `frame_bg`, `button_bg`) plus the per-enchant book art
under `textures/items/books/`.

### Enchanting books grid (`gardens_ebooks_form` / `gardens_ebooks_panel`)

One `SimpleForm` per page, sent by **gardens-enchants**' `BedrockEnchantingBooksMenu`. Buttons are sent
in one fixed block (padded with empty buttons to keep indices aligned):

- `0..SLOTS-1` — the 5×3 book tiles (`textures/items/books/<enchant_id>` icon + name; **read-only**,
  hover reveals the full lore). Tiles use `button.menu_select` with `focus_enabled: false` (the shop
  view-cell pattern) so tapping never reports a click or closes the form.
- `SLOTS` Prev, `SLOTS+1` Next (sent empty — no image — when unavailable so the pack's texture gate
  hides them), `SLOTS+2` Back (always shown; returns to the enchant menu). Send order is Prev/Next/Back
  but the pack lays the nav row out as **Prev, Back, Next**, so the leftmost slot is Prev when there's a
  previous page and Back otherwise (hidden nav buttons collapse), with Back always just left of Next.

Cells reuse the quests hover machinery (`gardens_ebooks_cell_content`: icon on top, a clipped name
below, and `our_lore@common.hover_text` bound to `#form_button_text` to reveal the whole
name + lore string on hover). Reuses the shop textures (`title_bar`, `frame_bg`, `cell_bg`,
`button_bg`, `nav_prev`, `nav_next`).

The enchant menu's own frame (`gardens_enchant_form`) gained a fourth `form_buttons` cell
(`collection_index 3`, an `enchant_books_btn` text button) that opens this catalogue; its window grew
to fit the extra footer button.

### Warps grid (`gardens_warps_form` / `gardens_warps_panel`)

One `SimpleForm` per view; `BedrockWarpsMenu` sends buttons in a single fixed block so the pack's
hardcoded cells resolve to the right actions (padding missing tiles/nav with empty buttons):

- `0..20` — the 7×3 warp head tiles (URL head icon + name label + hover details; click = travel).
- `21` Prev, `22` Next, `23` the **Yours** toggle.
- `24..30` — the 7 category tabs.

Head tiles use `FormImage.Type.URL` (`https://mc-heads.net/head/<uuid>`); the tile's `icon` control
binds both `#form_button_texture` and `#form_button_texture_file_system` so Bedrock loads the remote
image.

### Quests grid (`gardens_quests_form` / `gardens_quests_panel`)

One `SimpleForm` per page; `BedrockQuestsMenu` sends buttons in a single fixed block (padded with empty
buttons to keep indices aligned):

- `0..11` — the 4×3 quest tiles for the page (icon + name/progress; hover-only, no click).
- `12` Prev (far left), `13` Next (far left), `14` Leaderboard (far right). Prev/Next are sent as
  empty buttons — no image — when unavailable so the pack's texture gate hides them (e.g. Prev is absent
  on page 1); the hidden one collapses so the other stays flush-left.

Tiles are **read-only**: each cell's `main_button` uses `button.menu_select` with `focus_enabled:
false` (the shop view-cell pattern), so tapping a tile never reports a click or closes the form — only
Prev/Next (`button.form_button_click`) fire. Category icons come from `QuestIcons.java`
(`FormImage.Type.PATH` → `textures/gardens/quests/icon_<category>_task[_completed]`); the icon sits on
top and the tile shows a three-line clip of name + progress below it, revealing the full reward/status
on hover (`#form_button_text`). The plugin puts two blank lines between the progress and the hover-only
reward so the reward can't peek under a short (single-line) name. `#form_text` drives the footer stats
line (completed count + reset timer + page).

### Quest leaderboard (`gardens_qlb_form` / `gardens_qlb_panel`)

`BedrockQuestLeaderboardMenu` sends a 3×3 head-tile grid + a Back button:

- `0..8` — ranked player-head tiles (URL head icon + `#rank name` / quests label; hover-only). Empty
  ranks send the gray default icon with an "Unclaimed" label.
- `9` — Back (returns to the quests menu).

Tiles reuse the same hover-cell machinery as the quests grid (`gardens_qlb_cell_content`: URL head icon
on top, 3-line name clip below). `#form_text` drives the header line (the viewer's own rank).

The grid maps each cell to **two** Geyser buttons. The plugin sends buttons in three fixed blocks per
page: indices `0..SLOTS-1` = per-item "view" buttons (icon + price → lore), `SLOTS..2*SLOTS-1` =
matching "buy" buttons (the green Buy strip → buy window), then Prev / Next. Missing items/nav are
sent as empty buttons so indices stay aligned. The pack draws an item row (view buttons) followed by a
buy row (buy strips) for each grid row, so a cell's two halves resolve to its two collection indices.

### JSON UI gotchas learned here (read before editing)

- **Full override only.** A `_ui_defs.json`-registered "modifications" file did **not** apply on the
  target Bedrock version. A *complete* `ui/server_form.json` override does. (A partial/incomplete
  override blanks **all** forms server-wide, so keep it complete and gate custom layout on the token.)
- **`collection_name` must live on a `stack_panel`,** not a plain `panel`. With it on a plain panel,
  every cell's `collection_index` silently resolves to 0. The grid is therefore built from nested
  `stack_panel` rows (the pattern from the JSON-UI-5 sample), with `collection_index` on the leaves.
- **`grid` controls** auto-index correctly but can't easily give a cell two actions and don't scroll
  reliably — hence the pagination + explicit two-button layout.
- A control's `collection_index` only resolves when its **parent** carries `collection_name`.

## Per-item icons

`ShopItemIcons.java` maps Bukkit `Material` → Bedrock texture path. It's a curated subset; unmapped
materials (most blocks, custom/furniture items) fall back to `textures/gardens/shop/default_item` (the
gray placeholder). Extend `PATHS` to cover more items — verify each path against your Bedrock version.

`QuestIcons.java` maps each `TaskCategory` → its custom quest icon under `textures/gardens/quests/`
(`icon_<category>_task`, plus a `_completed` variant), matching the Java menu's `icon_<category>_task`
item naming so every category resolves to a real icon.

## Files

```
bedrock-resourcepack/
├── manifest.json
├── pack_icon.png                       # placeholder — replace with real art
├── font/glyph_E1.png                   # blank sheet so the U+E11x tokens render invisibly
├── ui/server_form.json                 # full vanilla override + gardens grid
├── textures/gardens/shop/
│   ├── default_item.png                # fallback item icon
│   ├── cell_bg.png                     # dark rounded card
│   ├── buy_button.png                  # green Buy strip
│   └── nav_prev.png / nav_next.png     # pagination arrows
└── textures/gardens/rtp/
    └── icon_<dimension>.png            # the six /rtp dimension cards (art + green "+" baked in)
```

## Installing on Geyser

1. Zip the **contents** of `bedrock-resourcepack/` (so `manifest.json` is at the zip root) as
   `gardens_shop.mcpack`, or use the prebuilt `gardens_shop.mcpack` in the repo root.
2. Drop it in Geyser's `packs/` folder and restart Geyser. Bedrock players download it on join.
3. Deploy the matching plugin jar — the pack and `BedrockShopMenu` must agree on the tokens, the
   `SLOTS` count (9×3), and the three-block button order.

```bash
cd bedrock-resourcepack && zip -r ../gardens_shop.mcpack . -x '*.DS_Store' && cd ..
```

## Known follow-ups (optional)

- Expand `ShopItemIcons` so fewer items show the gray placeholder.
- Lore-on-hover panel (currently lore shows on click, not hover).
- Theme the lore / buy-window / confirm screens (currently plain vanilla forms).
- Replace placeholder `pack_icon.png` and tune cell art.
