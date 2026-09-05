# Project Name: [Quick Contact Sheet]
**Primary Platform:** Android

## 1. High-Level App Concept
- **The Elevator Pitch:** A mobile app that lets users quickly add a contact widget to their home screen for easy access to starting a call, sending a text, or automatically opening a text thread with a pre-filled message.
- **The Core Value:** Saves time and effort by providing one-tap access to frequently contacted people.

## 2. Target Audience & Core Experience
- **Primary Users:** Universal for anyone who frequently sends the same messages or calls to a set of contacts.
- **Main Goal:** The user should be able to tap the widget and immediately access calling, texting, preset messages, and the contact details via a bottom sheet.

## 3. Screen-by-Screen Breakdown
- **Screen 1: Main Screen**
  - The main functionality of the app will come from the config screen after the user has added a contact widget to their home screen. So the main screen on app open should be a simple welcome screen.
  - Should have a simple message like "Add a contact widget to start using your quick contact sheet!"
  - A FAB that says "Add Widget" which will open the Android 'Add widget' bottom sheet so the user can immediately add the widget to their home screen.
  - Once the various global app settings are planned, this home screen can house those settings. Everything else will live in each widget's config screen.
- **Screen 2: Config Screen**
  - This screen should automatically open once the user has added a widget to their home screen.
  - Each widget and its settings should be individually unique. 
  - For the app v1, we'll keep the config process simple, then later develop custom settings. 
    * v1 config displays a 'Choose a contact' screen showing all of the user's contacts.
    * After selecting a contact, the widget background should update to display the contact's name and photo (if available). 
    * Specific widget size layouts to be determined by example screenshots.
- **Screen 3: Bottom Sheet**
  - First row should contain the contact's circular photo (if available) which opens the persons contact ontap, then a column of two filled icon buttons:
    * Button 1: "Call" - initiates a phone call to the contact.
    * Button 2: "Text" - opens the messaging app to the contact's conversation thread.
  - Second row is a scrollable list of bubbles containing user-created messages. On tap, each bubble should open the messaging app with the message pre-filled for the selected contact.
    * The bubbles should be rearrangeable by the user via drag-and-drop.
    * The last bubble is always the 'add/edit' icon button, allowing the user to quickly add or modify messages.
    * Tapping the 'add/edit' bubble should open a dialog or new screen where the user can add a new message or edit existing ones.

## 4. Technical Constraints (Keep It Simple)
- **Data Storage:** Save all user data locally on the device (no cloud database needed for version 1).
- **Design Style:** Modern, clean, dark mode by default, utilizing Google Material 3, Jetpack Compose and Glance. 

## 5. Milestones for the AI Agent
- [ ] Task 1: Initialize the basic project folder structure for Android.
- [ ] Task 2: Build Screen 1 (Main Screen) exactly to description.
- [ ] Task 3: Set up local storage capabilities for testing.
- [ ] Task 4: Build Screen 2 (Config Screen) exactly to description.
- [ ] Task 5: Build Widget Layouts exactly to description using context from the provided screenshots.
- [ ] Task 6: Build Screen 3 (Bottom Sheet) exactly to description using context from the provided screenshots.

## 6. Widget Layouts
- **Widget Size Width of 1 and any Height**
  - Displays the contact's circular photo only (if available). Centered within the widget, filling the width.
  - Tapping the widget opens the bottom sheet for quick actions.
- **Widget Size 2x1**
  - Displays dark widget background in a pill shape, filling the width of the widget. 
  - Left to right: contact's circular photo (if available), call icon button, text icon button.
  - Tapping the photo opens the bottom sheet.
  - Tapping the call or text icon buttons initiates the respective actions.
- **Widget Size 3x1**
  - Displays dark widget background in a pill shape, filling the width of the widget.
  - Left to right: contact's circular photo (if available), contact name in two lines, call icon button, text icon button.
  - Tapping the photo opens the bottom sheet.
  - Tapping the call or text icon buttons initiates the respective actions.
- **Widget Size Width of 4 or greater x 1**
  - Displays dark widget background in a pill shape, filling the width of the widget.
  - Left to right: contact's circular photo (if available), contact name in one line, call icon button, text icon button.
  - Tapping the photo opens the bottom sheet.
  - Tapping the call or text icon buttons initiates the respective actions.
- **Widget Size 2x2**
  - The contact's photo (if available) fills the available widget background, displays the contact's name over a slight black gradient in the top-left corner, and two quick action buttons (Call and Text) in the bottom-left corner.
  - Tapping the widget outside the buttons opens the bottom sheet.
- **Widget Size Larger Than 2x2**
  - Any widget size larger than 2x2 should follow a similar layout to the 2x2 widget, with the contact's photo filling the background, the contact's name over a slight black gradient in the top-left corner, and quick action buttons (Call and Text) in the bottom-left corner.
  - Tapping the widget outside the buttons opens the bottom sheet.