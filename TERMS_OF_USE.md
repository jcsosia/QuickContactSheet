# Terms of Use for Quick Contact Sheet

**Effective Date:** September 12, 2026  
**Last Updated:** September 12, 2026

Welcome to **Quick Contact Sheet** ("we", "us", "our", or the "Application"). These Terms of Use ("Terms") constitute a legally binding agreement between you ("you", "your", or "User") and the developers of Quick Contact Sheet regarding your access to and use of the Quick Contact Sheet mobile application and its associated home-screen widgets on the Android operating system.

PLEASE READ THESE TERMS OF USE CAREFULLY. BY DOWNLOADING, INSTALLING, ACCESSING, OR USING QUICK CONTACT SHEET, YOU ACKNOWLEDGE THAT YOU HAVE READ, UNDERSTOOD, AND AGREE TO BE BOUND BY THESE TERMS. IF YOU DO NOT AGREE TO THESE TERMS, DO NOT DOWNLOAD, INSTALL, OR USE THE APPLICATION.

---

## 1. Description of the Application

Quick Contact Sheet is an open-source Android utility that provides home-screen widgets (built with Jetpack Glance) and in-app configuration tools designed for fast, one-tap access to your device contacts, pre-configured communication shortcuts, and reusable text snippets.

### 1.1 Local & Offline Architecture
Quick Contact Sheet operates **strictly offline and locally** on your device. The Application:
* Does not require an account or registration.
* Does not host, collect, or transmit your personal data to any external server or cloud service.
* Does not request the Android `INTERNET` permission and structurally cannot transmit your information across a network.

### 1.2 System Intent Routing
Quick Contact Sheet acts as a visual shortcut and launcher. When you perform actions such as placing a call or sending a text message:
* The Application delegates the action to your device's native phone dialer (`android.intent.action.DIAL`) or messaging application (`android.intent.action.SENDTO`).
* Quick Contact Sheet does not directly place telephone calls, record conversations, transmit SMS/MMS messages independently in the background, or manage cellular connections.

---

## 2. Emergency Services Disclaimer (CRITICAL NOTICE)

**QUICK CONTACT SHEET IS NOT A TELECOMMUNICATIONS CARRIER OR EMERGENCY DISPATCH SERVICE.**

* **No Emergency Calling:** Quick Contact Sheet is not designed, intended, or authorized to support or carry emergency calls to hospitals, law enforcement agencies, medical care units, or any public safety answering points (e.g., 911 in the United States, 112 in the European Union, 999 in the United Kingdom, or local equivalents).
* **Do Not Rely on Widgets in Emergencies:** Home-screen widgets depend on operating system background processes, memory availability, and launcher states that may experience delays, restarts, or crashes. You must NEVER rely on Quick Contact Sheet or its widgets for life-critical, emergency, or time-sensitive communications.
* **Direct Access Required:** In any emergency situation, you must immediately access your device's native phone dialer or dedicated hardware emergency button directly.

---

## 3. Carrier Fees and Third-Party Services

When you use Quick Contact Sheet to initiate telephone calls or text messages:
* All calls and messages are transmitted across your cellular carrier's network or via third-party communication apps installed on your device.
* **Charges & Fees:** You are solely responsible for all cellular data charges, SMS/MMS messaging fees, roaming fees, and call rates imposed by your mobile network operator.
* **Third-Party Availability:** We are not responsible for the performance, delivery failures, availability, or policies of your mobile carrier, default SMS application, or dialer app.

---

## 4. Eligibility & Permitted Use

### 4.1 Eligibility
You must be at least the age of majority in your jurisdiction, or have reached the legal age required to consent to the use of digital services under applicable local law, to use this Application. If you are under the legal age, you may only use the Application under the supervision of a parent or legal guardian who agrees to be bound by these Terms.

### 4.2 License Grant
Subject to your compliance with these Terms and any applicable open-source licenses, we grant you a personal, revocable, non-exclusive, non-transferable, limited license to download, install, and use Quick Contact Sheet solely on Android devices that you own or control.

### 4.3 Prohibited Activities
You agree that you will **not**:
* Use the Application to harass, abuse, stalk, threaten, defame, or violate the legal rights of any person.
* Utilize the Application or its preset messaging features to generate spam, automated marketing blasts, unsolicited commercial communications, or deceptive messages.
* Use the Application for any illegal, unauthorized, or fraudulent purpose under local, state, national, or international law.
* Attempt to bypass, disable, or tamper with the security features or sandbox architecture of the Application or the Android operating system.
* Reverse engineer, decompile, or disassemble any part of the Application except to the extent permitted by applicable open-source software licenses or mandatory statutory law.

---

## 5. User Content, Contacts, and Local Backups

### 5.1 Device Contacts
To display your selected contacts on home-screen widgets, the Application requests the `READ_CONTACTS` permission. All contact reading occurs locally on your device. You represent and warrant that you have the right and authority to access and display any contact information you configure within the Application.

### 5.2 Preset Messages
You are solely responsible for the content, tone, and accuracy of any preset message snippets, templates, or notes you compose within Quick Contact Sheet. We exercise no editorial control over your text snippets and assume no liability for the content of messages sent using the Application.

### 5.3 Exported Backups
Quick Contact Sheet allows you to export your widget configurations and preset messages to a local JSON file.
* You are solely responsible for where you store, copy, share, or upload these backup files.
* If you export backups to insecure locations, unencrypted external drives, or third-party cloud drives, you assume all risks associated with the confidentiality and security of those files.
* We cannot restore lost data if you uninstall the Application or clear its storage without having created an external backup.

---

## 6. Intellectual Property & Open Source

### 6.1 Ownership
Except for third-party open-source libraries, the Application, its design, layouts, logos, source code, visual interfaces, and compilation are the intellectual property of the developers and are protected by applicable copyright, trademark, and intellectual property laws.

### 6.2 Open-Source Components
Quick Contact Sheet utilizes open-source libraries (including AndroidX, Jetpack Compose, Jetpack Glance, and related dependencies licensed under the Apache License, Version 2.0). Nothing in these Terms limits or overrides your rights under the applicable open-source licenses governing those components. Third-party license notices are available inside the Application under **About > Open Source Licenses**.

---

## 7. Disclaimer of Warranties ("AS IS")

TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW:

* QUICK CONTACT SHEET IS PROVIDED ON AN **"AS IS"** AND **"AS AVAILABLE"** BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE.
* WE EXPRESSLY DISCLAIM ALL WARRANTIES, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, NON-INFRINGEMENT, AND FREEDOM FROM COMPUTER VIRUSES OR DEFECTS.
* WE DO NOT WARRANT THAT:
  1. THE APPLICATION WILL MEET YOUR SPECIFIC REQUIREMENTS;
  2. THE APPLICATION OR ITS HOME-SCREEN WIDGETS WILL OPERATE UNINTERRUPTED, TIMELY, SECURELY, OR ERROR-FREE;
  3. WIDGET LAYOUTS WILL RENDER PERFECTLY ACROSS ALL THIRD-PARTY ANDROID LAUNCHERS OR CUSTOM OEM SKINS;
  4. ANY DEFECTS OR SYSTEM ERRORS WILL BE CORRECTED.

YOU EXPRESSLY ACKNOWLEDGE THAT YOUR USE OF THE APPLICATION IS AT YOUR SOLE RISK.

---

## 8. Limitation of Liability

TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL THE DEVELOPERS, CONTRIBUTORS, AFFILIATES, OR LICENSORS OF QUICK CONTACT SHEET BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, EXEMPLARY, OR PUNITIVE DAMAGES, INCLUDING BUT NOT LIMITED TO:
* LOSS OF PROFITS, DATA, USE, GOODWILL, OR BUSINESS REPUTATION;
* MISSED PHONE CALLS, DELAYED OR FAILED TEXT MESSAGES, OR INABILITY TO COMMUNICATE WITH CONTACTS;
* PERSONAL INJURY, PROPERTY DAMAGE, OR EMERGENCY SITUATIONS RESULTING FROM RELIANCE ON WIDGET SHORTCUTS;
* DEVICE CORRUPTION, CRASHES, OR OPERATING SYSTEM COMPATIBILITY ISSUES.

OUR TOTAL AGGREGATE LIABILITY TO YOU FOR ALL CLAIMS ARISING OUT OF OR RELATING TO THESE TERMS OR YOUR USE OF (OR INABILITY TO USE) THE APPLICATION SHALL NOT EXCEED THE AMOUNT YOU ACTUALLY PAID TO ACQUIRE THE APPLICATION, OR FIFTY UNITED STATES DOLLARS ($50.00 USD), WHICHEVER IS GREATER.

SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OF CERTAIN WARRANTIES OR THE LIMITATION OR EXCLUSION OF LIABILITY FOR INCIDENTAL OR CONSEQUENTIAL DAMAGES. ACCORDINGLY, SOME OF THE ABOVE LIMITATIONS MAY NOT APPLY TO YOU.

---

## 9. Indemnification

You agree to indemnify, defend, and hold harmless the developers, maintainers, and contributors of Quick Contact Sheet from and against any claims, liabilities, damages, losses, costs, expenses, or fees (including reasonable attorneys' fees) arising out of or relating to:
1. Your use or misuse of the Application;
2. Any violation by you of these Terms;
3. Any message content or communications dispatched by you using the Application;
4. Your violation of any rights of another person or entity.

---

## 10. Modifications to the Application and Terms

### 10.1 App Modifications
We reserve the right to modify, update, suspend, or discontinue any aspect of Quick Contact Sheet at any time without notice or liability. We are under no obligation to provide ongoing maintenance, technical support, or feature enhancements.

### 10.2 Changes to Terms
We may revise these Terms of Use from time to time. When changes are made, the revised version will be published in the Application repository and the "Last Updated" date will be updated. Your continued use of the Application after the effective date of any changes constitutes your acceptance of the amended Terms.

---

## 11. Termination

These Terms remain in effect until terminated by either you or us:
* **By You:** You may terminate these Terms at any time by permanently uninstalling and ceasing all use of the Application and deleting all local backup files.
* **By Us:** We may terminate or restrict your right to use the Application at any time without notice if you breach any provision of these Terms.

Upon termination, all licenses granted to you under these Terms immediately cease, while Sections 2, 5, 6, 7, 8, 9, 12, and 13 shall survive.

---

## 12. Governing Law and Jurisdiction

These Terms of Use shall be governed by and construed in accordance with the laws of the United States, without giving effect to any principles of conflicts of law. Any legal suit, action, or proceeding arising out of or related to these Terms or the Application shall be instituted exclusively in courts of competent jurisdiction.

---

## 13. Severability and Entire Agreement

* **Severability:** If any provision of these Terms is held to be invalid, illegal, or unenforceable, that provision shall be enforced to the maximum extent permissible, and the remaining provisions shall continue in full force and effect.
* **Entire Agreement:** These Terms of Use, together with our [Privacy Policy](PRIVACY_POLICY.md), constitute the entire and sole agreement between you and us concerning Quick Contact Sheet and supersede all prior or contemporaneous understandings or agreements.
* **No Waiver:** Our failure to exercise or enforce any right or provision of these Terms shall not operate as a waiver of such right or provision.

---

## 14. Contact and Feedback

If you have questions, feedback, bug reports, or legal inquiries regarding these Terms of Use or Quick Contact Sheet, please contact us via:

* **Email:** [support@sosiacollective.com](mailto:support@sosiacollective.com)
* **GitHub Repository:** [https://github.com/jcsosia/QuickContactSheet](https://github.com/jcsosia/QuickContactSheet)
