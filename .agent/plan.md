# Project Plan

Erweiterung der "CoffIO" App:
- Parameter "Mahlgrad" im Brühmenü und Datenmodell (Room, DataStore).
- Neue Seite "Charts" zur Visualisierung der Brühvorgänge pro Kaffeesorte über die Zeit.
- Integration von "Charts" ins Hauptmenü.

## Project Brief

# Project Brief: CoffIO Expansion (v2.0)

## Features
*   **Precision Brewing Parameters:** Addition of a "Grind Size" (Mahlgrad) setting to the brewing interface and the underlying `Brew` data model for expert-level control.
*   **Performance Analytics (Charts):** A new dedicated "Charts" module that visualizes brewing consistency and history for individual coffee varieties over time.
*   **Temporal Data Visualization:** Interactive time-series charts displaying data points along a chronological X-axis, enabling users to track the evolution of their technique.
*   **Enhanced Navigation Hub:** An updated main menu featuring a direct link to the Charts dashboard, providing a unified entry point for brewing, history, and analytics.

## High-Level Technical Stack
*   **Kotlin:** Core programming language.
*   **Jetpack Compose:** Modern UI toolkit for the Material 3 "White Theme" and Edge-to-Edge display.
*   **Room (via KSP):** Persistent relational database for storing `Brew` records, including the new Grind Size parameter.
*   **Jetpack DataStore:** Preferences storage for persisting the last-used Grind Size and other UI states.
*   **Kotlin Coroutines:** Asynchronous handling of database queries and chart data processing.
*   **Jetpack Navigation:** Multi-screen routing between the Menu, Brewing interface, and the new Charts page.
*   **Vico or Compose Charts:** Native Jetpack Compose library for high-performance time-series visualization.

## Implementation Steps
**Total Duration:** 45m 25s

### Task_1_Core_Infrastructure: Set up the Room database with entities (Brew, Coffee, Sieve), DAOs, and the DataStore manager for persisting last-used settings. Configure the Jetpack Navigation graph.
- **Status:** COMPLETED
- **Updates:** Room database (entities Brew, Coffee, Sieve) and DAOs created. DataStore manager for last-used settings implemented. Jetpack Navigation graph with routes for Home, Brewing, History, and Settings defined. Edge-to-Edge and Material 3 theme basics applied. Build passes.
- **Acceptance Criteria:**
  - Room database with Brew, Coffee, and Sieve entities is functional
  - DataStore correctly saves and retrieves user preferences
  - Navigation graph is defined for all main screens
  - Build passes
- **Duration:** 3m 51s

### Task_2_Main_Menu_And_History: Implement the Main Menu (Home) screen and the History screen. The Main Menu should feature 'Kaffeespezialität', 'History', and 'Settings' as per the design requirements.
- **Status:** COMPLETED
- **Updates:** Implemented Main Menu (Home) screen with 'Kaffeespezialität', 'History', and 'Settings' sections. Created History screen with a list of past brews from the Room database. Implemented `BrewWithCoffee` relation and `HistoryViewModel`. Navigation between Home, Brewing, History, and Settings works. Material 3 and Edge-to-Edge support applied. Build passes.
- **Acceptance Criteria:**
  - Main Menu UI matches Material 3 guidelines with a lean, white aesthetic
  - History screen displays a list of past brewing sessions from the database
  - Navigation between screens works correctly
- **Duration:** 5m 11s

### Task_3_Brewing_Interface: Develop the Brewing Menu screen. Include dropdowns for Coffee/Sieve selection, input fields for brewing parameters (temp, weight, yield, pressure, milk), and the 'Done' button to save logs.
- **Status:** COMPLETED
- **Updates:** Implemented Brewing Menu screen with dropdowns for Coffee and Sieve selection. Added 'Add' icon next to dropdowns for creating new entities. Input fields for Temperature, Coffee Weight, Target Yield, Tamper Pressure, Milk Volume, and Actual Yield with decimal validation. On load, last-used settings are fetched from DataStore. Clicking 'Done' saves the Brew record to Room, updates DataStore, and navigates back to Home. Material 3 'lean, white' design applied. Build passes.
- **Acceptance Criteria:**
  - Coffee and Sieve selection works via dropdowns
  - Inputs for brewing variables are implemented with validation
  - The 'Done' button saves the Brew record to Room and updates last-used settings in DataStore
  - Actual yield input is functional post-brew
- **Duration:** 9m 22s

### Task_4_Final_Polish_And_Verify: Refine the theme to ensure a lean, white Material 3 aesthetic with full Edge-to-Edge support. Create an adaptive app icon and perform a final stability run.
- **Status:** COMPLETED
- **Updates:** Final refinement complete. App uses a vibrant Material 3 color scheme with white dominance. Full Edge-to-Edge display is implemented and verified. Adaptive app icon is created and structured for modern launchers. Application is stable, functionally complete, and verified by critic_agent. Navigation flow between Home, Brewing, and History is seamless. Room database and DataStore persistence are confirmed.
- **Acceptance Criteria:**
  - App uses a vibrant Material 3 color scheme with white dominance
  - Full Edge-to-Edge display is implemented
  - Adaptive app icon is created
  - Application is stable (no crashes)
  - Alignment with user requirements confirmed by critic_agent
- **Duration:** 3m 52s

### Task_5_Expand_Data_And_UI: Update the Room 'Brew' entity and DataStore to include the 'Grind Size' parameter. Update the Brewing Menu UI to include a 'Grind Size' input field and add the 'Charts' entry point to the Main Menu.
- **Status:** COMPLETED
- **Updates:** Room database (Brew entity) updated with `grindSize` field. DataStore manager for last-used settings updated to persist `grindSize`. Brewing UI expanded to include functional 'Mahlgrad' (Grind Size) input. Main Menu (Home) screen updated to include a 'Charts' entry point with a bar chart icon. Navigation graph updated with a route for the 'Charts' screen. Build passes.
- **Acceptance Criteria:**
  - Brew entity updated with grindSize field
  - DataStore persists last-used grindSize
  - Brewing UI includes functional Grind Size input
  - Charts button added to Main Menu
  - Build passes
- **Duration:** 6m 28s

### Task_6_Charts_And_Verify: Implement the Charts screen using a Compose charting library to visualize brewing history per coffee variety over time. Perform a final Run and Verify for stability and requirements alignment.
- **Status:** COMPLETED
- **Updates:** Charts screen implemented with a coffee selection dropdown and a custom-drawn time-series chart (Yield over Time) using the Compose Canvas API. A Statistics summary card was added to provide averages and session counts. The Main Menu (Home) now includes a 'Charts' entry point. 'Grind Size' (Mahlgrad) parameter fully integrated into the Brewing UI and persistence layers (Room, DataStore). All features verified by critic_agent for stability and Material 3 design compliance. Build passes.
- **Acceptance Criteria:**
  - Charts screen displays time-series data correctly
  - Navigation to and from Charts screen works
  - Make sure all existing tests pass
  - Build pass
  - App does not crash
  - Alignment with user requirements confirmed by critic_agent
- **Duration:** 16m 41s

