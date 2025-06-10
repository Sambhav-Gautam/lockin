# LockIn

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-1.0.0-green.svg)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)
![Android](https://img.shields.io/badge/platform-Android-blue.svg)

---

![LockIn](https://media.giphy.com/media/3oEjI6SIIHBdRxXI40/giphy.gif)

> **LockIn** is an unbreakable self-discipline app for Android, designed to block distractions, ban pornographic content, and cultivate life-changing habits.

---

## Features

✅ **Distraction Blocking**  
Blocks distracting apps permanently with a secure, tamper-resistant password salt.

✅ **Pornographic Content Filtering**  
Allows only whitelisted browsers like Spin, ensuring a safe browsing experience.

✅ **Alarm with Affirmation Requirement**  
Alarm that keeps ringing until you type a powerful, long affirmation text to start your day right.

✅ **Monochrome Grayscale Mode**  
Turns your screen grayscale to reduce visual stimulation and improve focus.

✅ **Minimalist App Icons**  
Replaces colorful icons with minimalist, grayscale versions for reduced app temptation.

---

## Project Structure

```
com.example.lockin
├── data
│   ├── model
│   │   ├── AppInfo.kt
│   │   └── LockInUiState.kt
│   └── preferences
│       └── EncryptedPrefsHelper.kt
├── service
│   ├── AppBlockAccessibilityService.kt
│   ├── AppBlockService.kt
│   └── AppBlockWorker.kt
├── ui
│   ├── LockInApp.kt
│   ├── MainScreen.kt
│   └── AppPickerDialog.kt
├── viewmodel
│   ├── LockInViewModel.kt
│   └── LockInViewModelFactory.kt
└── MainActivity.kt
```

---

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/lockin.git
   ```
2. Open the project in Android Studio.
3. Build and run on your device.

> **Note:** This app requires **Device Owner permissions** to lock certain features and ensure robustness.

---

## Roadmap

* [x] Basic distraction blocking  
* [x] Browser whitelisting  
* [x] Affirmation alarm  
* [ ] Advanced tamper detection  
* [ ] Custom launcher with minimalist icons  
* [ ] Cloud sync for settings and affirmations  

---

## Contributing

We welcome contributions to enhance LockIn’s capabilities! Please fork the repository and create a pull request.

---

## License

This project is licensed under the [MIT License](LICENSE).

---

> “Transform your life by removing distractions and building habits that last.”

![Focus](https://media.giphy.com/media/l0MYEqEzwMWFCg8rm/giphy.gif)

---
