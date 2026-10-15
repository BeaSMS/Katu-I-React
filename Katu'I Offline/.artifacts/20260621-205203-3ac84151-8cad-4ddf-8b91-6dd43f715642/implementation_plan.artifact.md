# Implementation Plan - Vital Signs (Vitals) Screen Updates

Update the Vitals screen to support granular registration, age-based visibility for height, and time-based graphs (Daily, Weekly, Monthly).

## User Review Required

- **Graph Data Grouping**: For the daily view, data will be grouped by hour. For weekly and monthly views, data will be grouped by day. If multiple records exist for the same period, the average value will be displayed.
- **Heart Rate (Batimentos)**: Added as a separate registration option in the FAB menu, separate from Blood Pressure, as requested.

## Proposed Changes

### Data & ViewModel

#### [VitalsViewModel.kt](file:///C:/Users/Yue/AndroidStudioProjects/Katu'I Offline/app/src/main/java/com/katui/offline/ui/viewmodel/VitalsViewModel.kt)

- Inject `UsuarioRepository` to fetch user age.
- Add `val usuario: StateFlow<Usuario?>`.
- Add `val selectedTimeRange: MutableStateFlow<TimeRange>` (DAY, WEEK, MONTH).
- Create a transformed flow of `vitals` that groups data based on `selectedTimeRange` for chart consumption.

---

### UI Components

#### [VitalsScreen.kt](file:///C:/Users/Yue/AndroidStudioProjects/Katu'I Offline/app/src/main/java/com/katui/offline/ui/screens/VitalsScreen.kt)

- **FAB Menu**:
    - Add separate actions for "Saturação", "Temperatura", and "Batimentos".
    - Logic to hide "Altura" if `usuario.idade > 18` or `null`.
- **AddSpecificVitalsDialog**:
    - Update to handle new registration types independently.
- **VitalsTrendCard**:
    - Add a time range selector (Dia, Semana, Mês).
    - Expand `charts` list: "Pressão", "Glicose", "Peso", "Altura", "Batimentos", "Saturação", "Temperatura".
- **SimpleLineChart**:
    - Rewrite to support time-based X-axis.
    - Implement grouping logic (hourly for Day, daily for Week/Month).

## Verification Plan

### Automated Tests
- Run `gradle_build` to ensure project stability.

### Manual Verification
1. **Age-based Logic**:
    - Edit user profile to age 10: Verify "Altura" appears in Vitals FAB.
    - Edit user profile to age 25: Verify "Altura" is hidden.
2. **Registration**:
    - Register each vital type (Glicose, Pressão, Batimentos, Saturação, Temperatura, Peso).
    - Verify they appear correctly in the history list.
3. **Charts**:
    - Verify that each vital type has its own chart.
    - Toggle between Day, Week, and Month views.
    - Ensure the X-axis labels and data points reflect the selected period.
4. **Data Grouping**:
    - Register two Temperature readings within the same hour/day.
    - Verify the graph shows the average (or distinct points depending on final implementation).
