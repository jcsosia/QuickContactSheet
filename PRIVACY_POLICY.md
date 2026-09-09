# Privacy Policy for Quick Contact Sheet

**Effective Date:** September 9, 2026

This Privacy Policy explains how **Quick Contact Sheet** ("we", "us", or "our") handles your information when you use our Android application. 

Quick Contact Sheet is designed with privacy as a foundational principle. **The app operates entirely offline and does not connect to the internet.** We do not collect, transmit, share, or sell your personal data.

## 1. Data Access and Usage

To provide its core functionality, Quick Contact Sheet requires access to certain information locally on your device.

### 1.1 Contacts Data (Sensitive Information)
The app requests the **Read Contacts** (`android.permission.READ_CONTACTS`) permission.
* **What we access:** When you grant this permission, the app reads contact IDs, names, phone numbers, labels (e.g., Mobile, Home), and contact photo URIs stored on your device.
* **Why we access it:** This data is required solely to let you select contacts and display them on your home screen widgets for quick access.
* **Data Flow & Sharing:** Your contact data is processed strictly on your device. It is never transmitted to external servers, nor is it shared with any third parties. The app structurally cannot transmit data over a network, as it does not request internet permissions.

### 1.2 Calling and Messaging Interactions
Quick Contact Sheet acts as a shortcut to your device's native communication apps.
* **Calling:** When you tap a contact to make a call, the app passes the phone number to your device's default dialer using a standard system intent (`android.intent.action.DIAL`). The app does not place the call directly and does not have access to your call logs.
* **Messaging:** When you tap to send a message, the app passes the phone number and any pre-configured message text to your device's default SMS application (`android.intent.action.SENDTO`). The app does not send SMS messages directly in the background and does not have access to your message history.

### 1.3 Haptic Feedback
The app requests the **Vibrate** (`android.permission.VIBRATE`) permission strictly to provide physical haptic feedback when you interact with the widgets and app interfaces.

## 2. Local Storage and Backups

When you configure widgets and create pre-written messages, this configuration data is saved locally on your device.
* **Manual Backups:** You have the option to manually export your app settings and widget presets. This creates a local JSON file on your device's storage. 
* We do not have access to these backups, and they are never automatically uploaded to any cloud service. You are solely responsible for where you store or share your exported backup files.

## 3. Third-Party Services and Analytics

Quick Contact Sheet contains **no** third-party SDKs, analytics trackers, crash reporters, or advertising frameworks. Your usage habits, interaction data, and personal information are not monitored or monetized in any way.

## 4. Data Retention and Deletion

Because all data handling occurs locally on your hardware:
* **Retention:** Your widget configurations and selected contacts remain on your device as long as the app is installed.
* **Deletion:** You maintain full control over your data. You can delete all data associated with Quick Contact Sheet instantly by clearing the app's storage in your Android System Settings or by uninstalling the application.

## 5. Security

We rely on the Android operating system's built-in application sandboxing to protect your data. Furthermore, the absence of internet access permissions within the app's architecture guarantees that your sensitive contact information cannot be leaked over a network by the application.

## 6. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect changes in our app's features or legal requirements. Any changes will be reflected in this document, and the "Effective Date" at the top will be updated accordingly.

## 7. Contact Us

If you have any questions, concerns, or inquiries regarding this Privacy Policy or our data handling practices, please contact us via our GitHub repository issues page.
