#include <Arduino.h>
#include <BluetoothSerial.h>
#include <Adafruit_NeoPixel.h>
#include "esp_bt_device.h"

// Configuratie
#define LED_PIN     2
#define NUM_LEDS    21

BluetoothSerial SerialBT;
Adafruit_NeoPixel strip(NUM_LEDS, LED_PIN, NEO_GRB + NEO_KHZ800);

// Maneuver codes (zoals gedefinieerd in Google Maps SDK)
#define TURN_LEFT 6
#define TURN_RIGHT 7

// Parameters
const int DISTANCE_THRESHOLD = 50; // Afstand waaronder animatie-pijl getoond wordt
const int MAX_DISTANCE = 300;      // Afstand voor maximale progressie (alles vol)

// Functies
void handleInstruction(int instruction, int distance);
void fillSolid(uint32_t color);
void animateArrowLeft(uint32_t color);
void animateArrowRight(uint32_t color);
int mapDistanceToLeds(int distance, int maxDistance);

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
  strip.clear();
  strip.show();
  strip.setBrightness(5);
}

void loop() {
  if (SerialBT.available()) {
    String input = SerialBT.readStringUntil('\n');
    Serial.print("Ontvangen: ");
    Serial.println(input);

    int instruction = 0;
    int distance = 0;

    if (sscanf(input.c_str(), "Instructie: %d, Afstand: %dm", &instruction, &distance) == 2) {
      handleInstruction(instruction, distance);
    } else {
      Serial.println("Ongeldig formaat ontvangen.");
    }
  }

  delay(50);
}

void handleInstruction(int instruction, int distance) {
  strip.clear();

  if (instruction == -1) {
    for (int i = 0; i < 3; i++) {
      fillSolid(strip.Color(0, 255, 0)); delay(150);
      fillSolid(strip.Color(0, 0, 0)); delay(150);
    }
    return;
  }

  if (distance < DISTANCE_THRESHOLD) {
    if (instruction == TURN_LEFT) animateArrowLeft(strip.Color(0, 255, 0));
    else if (instruction == TURN_RIGHT) animateArrowRight(strip.Color(0, 255, 0));
    else fillSolid(strip.Color(1, 0, 0));
    return;
  }

  int ledsToLight = mapDistanceToLeds(distance, MAX_DISTANCE);

  if (instruction == TURN_LEFT) {
    for (int i = 0; i < ledsToLight; i++) {
      strip.setPixelColor(i, strip.Color(0, 80, 0));
    }
  } else if (instruction == TURN_RIGHT) {
    for (int i = 0; i < ledsToLight; i++) {
      strip.setPixelColor(NUM_LEDS - 1 - i, strip.Color(0, 80, 0));
    }
  } else {
    // fillSolid(strip.Color(50, 50, 50));
    strip.clear();
    strip.show();
  }

  strip.show();
}

void fillSolid(uint32_t color) {
  for (int i = 0; i < strip.numPixels(); i++) {
    strip.setPixelColor(i, color);
  }
  strip.show();
}

void animateArrowLeft(uint32_t color) {
  for (int offset = 0; offset < NUM_LEDS / 2; offset++) {
    strip.clear();
    for (int i = 0; i < 3; i++) {
      int pos = offset - i;
      if (pos >= 0) strip.setPixelColor(pos, color);
    }
    strip.show();
    delay(60);
  }
}

void animateArrowRight(uint32_t color) {
  for (int offset = NUM_LEDS - 1; offset >= NUM_LEDS / 2; offset--) {
    strip.clear();
    for (int i = 0; i < 3; i++) {
      int pos = offset + i;
      if (pos < NUM_LEDS) strip.setPixelColor(pos, color);
    }
    strip.show();
    delay(60);
  }
}

int mapDistanceToLeds(int distance, int maxDistance) {
  int capped = min(distance, maxDistance);
  return map(maxDistance - capped, 0, maxDistance, 0, NUM_LEDS / 2);
}