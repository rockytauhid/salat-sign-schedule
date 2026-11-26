# SalatSchedule — Android TV Screensaver (`DreamService`)

SalatSchedule registers a strictly silent, dark-mode Android TV screensaver (`com.salatschedule.tv.screensaver.SalatScreensaverService`) that rotates up to three slides while your TV is idle:

1. **Prayer Schedule** — large clock (no seconds, 12h/24h per your app setting), Gregorian and Hijri date, 5 daily prayers + Sunrise (Friday Dhuhr stays labeled Dhuhr), with the current active prayer highlighted and countdown to the next prayer.
2. **Islamic Calendar** — recent past (within 14 days), TODAY, and upcoming Islamic dates (skipped if no events are enabled).
3. **Quran Verse** — one offline verse per rotation from 63 curated verses (Arabic + translation in your app language, Auto duration, never truncated).

Any remote key press or touch closes the screensaver immediately. Content shifts subtly (≤ 8 px) every minute to help protect TV panels from burn-in.

> **Prayer-Time Alert Banner & Adhan Behavior:** By design, the 5-minute visual **Prayer-Time Alert Banner** only appears on SalatSchedule's main Schedule screen and does not interrupt the ambient screensaver. Scheduled **Adhan audio** and **15-minute pre-prayer notifications** continue to fire normally while the screensaver is active.

---

## How to Enable on Android TV

On classic Android TV firmware (such as Sony Bravia running Android 8.0+):

1. Open **Settings** on your TV.
2. Go to **Device Preferences → Screen saver → Screen saver**.
3. Select **SalatSchedule**.

You can also tap **Open TV Screen Saver Settings** inside **SalatSchedule → Settings → Screensaver → How to Set as Screensaver** (when supported by the TV).

---

## Enabling on Google TV Devices That Hide Third-Party Screensavers

Some newer Google TV builds only show Google's built-in Ambient Mode options in the TV Settings menu. On those devices, you can still select SalatSchedule's `DreamService` once over ADB from a computer:

```bash
adb shell settings put secure screensaver_components com.salatschedule.tv/com.salatschedule.tv.screensaver.SalatScreensaverService
adb shell settings put secure screensaver_enabled 1
```

> **Note:** Availability of third-party screensavers varies by TV manufacturer and firmware. SalatSchedule does not request privileged system permissions and cannot force itself as the active screensaver automatically. Stock **Amazon Fire TV** firmware uses Amazon's proprietary Ambient Experience and does not launch standard Android `DreamService` screensavers on idle (the in-app **Preview Screensaver** still works on Fire TV).

---

## Quran Text & Translation Credits

- **Quran Arabic text:** Tanzil.net (`http://tanzil.net`)
- **English translation:** Saheeh International, via Tanzil.net
- **Indonesian translation:** Kementerian Agama Republik Indonesia
