# Implementation Plan: Issue #6 - Resize size of the text

## Analysis
- **Problem:** The typing text is fixed and small. Users want to resize it using `Ctrl + scroll wheel`, similar to the IDE editor behavior.
- **Requirement:** 
  1. Catch `MouseWheelEvent` with `Ctrl` modifier.
  2. Increase/Decrease the font size of the `JTextPane`.
  3. Re-calculate the dialog's preferred size based on the new font metrics.
  4. Update the dialog layout to reflect the size change.
- **User Feedback:** Use default settings for font size range (standard IDE/Swing behavior).

## Importance Assessment
- **Rating:** High
- **Reasoning:** Improving readability is crucial for a typing trainer. Fixed small text can cause eye strain and makes the plugin less accessible.

## Time Estimation
- **Estimate:** 3 hours
- **Breakdown:**
  - Researching font resizing in `JTextPane` (0.5h)
  - Implementing `MouseWheelListener` and font logic (1h)
  - Integrating with dynamic dialog resizing (0.5h)
  - Testing and refinement (1h)

## Proposed Implementation Steps
1. **Add `MouseWheelListener` to `textPane`:**
   - Detect `e.isControlDown`.
   - Update a `currentFontSize` variable in `TypingDialog`.
2. **Update Font:**
   - Create a new `Font` based on `currentFontSize`.
   - Set the new font to `textPane`.
3. **Trigger Re-layout:**
   - Refactor the dynamic size calculation into a reusable method.
   - Call this method when the font changes.
   - Adjust the `TypingDialog` window size dynamically.
4. **Validation:**
   - Verify that the text scales correctly.
   - Verify that the dialog expands/shrinks as the text grows/shrinks.
   - Check that it respects screen boundaries.

## Clarification Questions
- ~~Is there a specific minimum or maximum font size desired?~~ (User confirmed: use defaults).
- Should the font size be persisted across sessions? (Not required by issue description, but could be a future enhancement).
