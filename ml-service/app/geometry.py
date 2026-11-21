"""Shared geometry of the printed sheet: the template drawer and the extractor must agree."""

PAGE_W, PAGE_H = 1654, 2339          # A4 at 200 dpi
COLS, ROWS = 8, 12
CELL_W, CELL_H = 186, 164
GRID_X = (PAGE_W - COLS * CELL_W) // 2
GRID_Y = 190
LABEL_H = 30                          # printed label strip at the top of each cell
BASELINE = 124                        # baseline y inside a cell (from the cell top)

CORNER = 56                           # side of the four black registration squares
CORNER_MARGIN = 60
CORNER_CENTERS = [                    # TL, TR, BR, BL
    (CORNER_MARGIN + CORNER // 2, CORNER_MARGIN + CORNER // 2),
    (PAGE_W - CORNER_MARGIN - CORNER // 2, CORNER_MARGIN + CORNER // 2),
    (PAGE_W - CORNER_MARGIN - CORNER // 2, PAGE_H - CORNER_MARGIN - CORNER // 2),
    (CORNER_MARGIN + CORNER // 2, PAGE_H - CORNER_MARGIN - CORNER // 2),
]

# 8-bit sheet id marker: 4 bits id + 4 inverted bits (so a blank or rotated page never validates).
MARK_N, MARK_SIZE, MARK_STEP = 8, 34, 44
MARK_X = PAGE_W - GRID_X - MARK_N * MARK_STEP
MARK_Y = 122


def cell_origin(index: int) -> tuple[int, int]:
    r, c = divmod(index, COLS)
    return GRID_X + c * CELL_W, GRID_Y + r * CELL_H
