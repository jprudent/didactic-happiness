#define X_AXIS_PIN 1
#define Y_AXIS_PIN 0
#define JOYSTICK_ACTIVE_LIMIT 75
#define JOYSTICK_Z_PIN 13

void initJoystick() {
  pullupMode(JOYSTICK_Z_PIN);
}

boolean isJoystickUp() {
  return readJoystick(Y_AXIS_PIN) > JOYSTICK_ACTIVE_LIMIT;
}

boolean isJoystickDown() {
  return readJoystick(Y_AXIS_PIN) < -JOYSTICK_ACTIVE_LIMIT;
}

boolean isJoystickRight() {
  return readJoystick(X_AXIS_PIN) > JOYSTICK_ACTIVE_LIMIT;
}

boolean isJoystickLeft() {
  return readJoystick(X_AXIS_PIN) < -JOYSTICK_ACTIVE_LIMIT;
}

boolean isJoystickZ() {
  return isBtnPressed(JOYSTICK_Z_PIN);
}

int readJoystick(int pin) {
  const int val = analogRead(pin);
  return map(val, 0, 1023, -100, 100);
}

