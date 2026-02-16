# AI Agent Instructions for Issue Processing

As an AI agent working on this project, you must follow these instructions when a new GitHub issue is identified for processing.

## 1. Issue Analysis
- **Importance Assessment:** Evaluate the importance of the issue based on its impact on user experience, stability, or project goals. Use a scale of Low, Medium, High, or Critical.
- **Time Estimation:** Provide a rough estimate of how much time (in hours or days) it will take to implement the solution.
- **Clarification:** If the issue description is missing critical information or is ambiguous, identify specific questions to ask the user.

## 2. Planning and Storage
- Create a detailed implementation plan.
- Store this information in the `plans/issue/` directory.
- Use a filename format: `issue-<number>-plan.md`.
- **Note:** The `plans/issue/` folder and this `AGENTS.md` file are ignored by version control to keep the repository clean of temporary AI artifacts.

## 3. Communication
- Add a polite comment to the GitHub issue.
- Start by acknowledging that the issue is very important (if applicable).
- State that it has been taken into work or is being analyzed.
- Ask any required clarification questions.

## 4. Implementation
- Refer to the stored plan in `plans/issue/` before starting the implementation.
- Update the plan if the implementation strategy changes.
