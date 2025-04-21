#include <Arduino.h>
#include <BluetoothSerial.h>
#include <Adafruit_NeoPixel.h>
#include "esp_bt_device.h"

// Configuratie
#define LED_PIN     2       // Pas aan naar jouw LED-data pin
#define NUM_LEDS    24

BluetoothSerial SerialBT;
Adafruit_NeoPixel strip(NUM_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);

// Functie-declaraties
void handleInstruction(int instruction, int distance);
void colorWipe(uint32_t color, int wait);
void rotateLeft(uint32_t color);
void rotateRight(uint32_t color);
void fillSolid(uint32_t color);

// Maneuver codes (zoals gedefinieerd in Google Maps SDK)
#define TURN_LEFT 6
#define TURN_RIGHT 7

// Variabelen voor afstand en instructie
int previousInstruction = -1;
int previousDistance = 0;
const int DISTANCE_THRESHOLD = 50; // Drempel voor bochten (in meters)

void setup() {
  Serial.begin(115200);
  SerialBT.begin("ESP32test");
  Serial.println("Bluetooth gestart.");

  const uint8_t* mac = esp_bt_dev_get_address();
  char macStr[18];
  snprintf(macStr, sizeof(macStr), "%02X:%02X:%02X:%02X:%02X:%02X",
           mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
  Serial.print("ESP32 Bluetooth MAC Address: ");
  Serial.println(macStr);

  strip.begin();
  strip.show(); // Alle leds uit
  strip.setBrightness(20); // Pas eventueel aan
}

void loop() {
  if (SerialBT.available()) {
    String input = SerialBT.readStringUntil('\n');
    Serial.print("Ontvangen: ");
    Serial.println(input);

    int instruction = 0;
    int distance = 0;

    if (sscanf(input.c_str(), "Instructie: %d, Afstand: %dm", &instruction, &distance) == 2) {
      // Controleer of de instructie anders is dan de vorige en dat de afstand onder de drempel is
      handleInstruction(instruction, distance);
    } else {
      Serial.println("Ongeldig formaat ontvangen.");
    }
  }

  delay(50);
}

// Verwerk navigatie-instructie
void handleInstruction(int instruction, int distance) {
  strip.clear();

  if (instruction == -1) {
    // Bestemming bereikt – groen flitsen
    for (int i = 0; i < 3; i++) {
      colorWipe(strip.Color(0, 255, 0), 20); // Groen voor bestemming
      colorWipe(strip.Color(0, 0, 0), 20); // Lichten uit
    }
  } else if (instruction == TURN_LEFT) {
    if (distance < DISTANCE_THRESHOLD) {
      // Actieve animatie voor linksaf als de afstand laag genoeg is
      rotateLeft(strip.Color(0, 255, 0)); // Groen voor linksaf
    } else {
      fillSolid(strip.Color(0, 255, 0)); // Neutrale kleur als afstand groot is
    }
  } else if (instruction == TURN_RIGHT) {
    if (distance < DISTANCE_THRESHOLD) {
      // Actieve animatie voor rechtsaf als de afstand laag genoeg is
      rotateRight(strip.Color(0, 255, 0)); // Groen voor rechtsaf
    } else {
      fillSolid(strip.Color(0, 255, 0)); // Neutrale kleur als afstand groot is
    }
  } else {
    fillSolid(strip.Color(255, 255, 255)); // Onbekend = wit
  }

  strip.show();
}

// Helfuncties
void colorWipe(uint32_t color, int wait) {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, color);
    strip.show();
    delay(wait);
  }
}

void rotateRight(uint32_t color) {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.clear();
    strip.setPixelColor(i, color);
    strip.show();
    delay(40);
  }
}

void rotateLeft(uint32_t color) {
  for (int i = strip.numPixels() - 1; i >= 0; i--) {
    strip.clear();
    strip.setPixelColor(i, color);
    strip.show();
    delay(40);
  }
}

void fillSolid(uint32_t color) {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, color);
  }
  strip.show();
}
