# FarmConnect Android (Java, MVVM, Retrofit)

1. Install Android Studio (Koala or newer, JDK 17 bundled).
2. File > Open > select this `farmconnect-android` folder. Let Gradle sync finish.
3. Start the backend first (`mvn spring-boot:run`).
4. Run on an emulator. Backend URL is set in `data/ApiClient.java` (`10.0.2.2` = your PC from the emulator).
5. Real phone: put your PC's LAN IP in `BASE_URL`, same Wi-Fi, allow port 8080 in the firewall.

Flow: Register as FARMER (create farm, add products, handle orders) or BUYER (browse, cart, wishlist, order).
