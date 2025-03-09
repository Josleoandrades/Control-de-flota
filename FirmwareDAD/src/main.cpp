#include <HTTPClient.h>
#include "ArduinoJson.h"
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <PubSubClient.h>
#include <Adafruit_Sensor.h>
#include <pwmWrite.h>
#include <DHT.h>
#include <DHT_U.h>
#include <TinyGPSPlus.h>
#include <SoftwareSerial.h>
#include <Wire.h>



int idSensor;
float actuador;
int year;
char fechaHora[20];
float latitude, longitude;
int16_t AcX, AcY, AcZ, Tmp, GyX, GyY, GyZ;
float ax, ay, az, gx, gy, gz;
float STATUS;
bool STATUSBINARY;


// Replace 0 by ID of this current device
const int DEVICE_ID = 1;
const int SENSOR_GPS_ID = 1369;
const int SENSOR_AC_ID = 1369;
const int ACTUADOR_ID = 1369;

float valueSensor;


int test_delay = 1500; // so we don't spam the API
boolean describe_tests = true;

// Replace 0.0.0.0 by your server local IP (ipconfig [windows] or ifconfig [Linux o MacOS] gets IP assigned to your PC)




String serverName = "http://172.20.10.2:8080/";




HTTPClient http;

// Replace WifiName and WifiPassword by your WiFi credentials
#define STASSID "iPhone de Jose"    //"Your_Wifi_SSID"
#define STAPSK "josejr66" //"Your_Wifi_PASSWORD"





#define DHTTYPE DHT22
#define DHTPIN 2 // pin de placa, donde pone G2
#define LED_PIN 27 // pin de placa, donde pone G27
#define TEMPERATURE_THRESHOLD 25.0
TinyGPSPlus gps;
SoftwareSerial serialgps(16, 17);

const int MPU_ADDR = 0x68;     // dirección del dispositivo en el bus I2C
const int WHO_AM_I = 0x75;     // registro de identificación del dispositivo
const int PWR_MGMT_1 = 0x6B;   // registro de gestión de energía 
const int GYRO_CONFIG = 0x1B;  //  registro de configuración del giroscopio
const int ACCEL_CONFIG = 0x1C; // registro de configuración del acelerómetro
const int ACCEL_XOUT = 0x3B;   //  registro de lectura de datos del acelerómetro en el eje X

const int acData_pin = 5; 
const int acClock_pin = 6; 

// NTP (Net time protocol) settings
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);








void initI2C()
{
  //Serial.println("---inside initI2C");
  Wire.begin(acData_pin, acClock_pin);
}

void writeRegMPU(int reg, int val) //Escribe un valor en un reg especifico del MPU
{
  Wire.beginTransmission(MPU_ADDR);  // Inicia la comunicación I2C con el MPU6050 en la dirección especificada (MPU_ADDR)
  Wire.write(reg);                   // Envía el registro al que se desea acceder (registro donde escribirás el valor)
  Wire.write(val);                   // Envía el valor que deseas escribir en ese registro
  Wire.endTransmission(true);        // Finaliza la transmisión, completando la operación de escritura
}


uint8_t readRegMPU(uint8_t reg) // Funcion para leer un registro
{
  uint8_t data;
  Wire.beginTransmission(MPU_ADDR);   // Inicia la comunicación I2C con el MPU6050
  Wire.write(reg);                    // Envía el registro del cual se desea leer
  Wire.endTransmission(false);        // Termina la transmisión pero mantiene la comunicación abierta para leer
  Wire.requestFrom(MPU_ADDR, 1);      // Solicita 1 byte del registro solicitado
  data = Wire.read();                 // Lee el byte recibido y lo guarda en la variable 'data'
  return data;                         // Devuelve el byte leído desde el registro
}


/*
 * Función para buscar el sensor en la dirección 0x68
 */
void findMPU(int mpu_addr)
{
  Wire.beginTransmission(MPU_ADDR);
  int data = Wire.endTransmission(true);

  if (data == 0)
  {
    Serial.print("Dispositivo encontrado en la direccion: 0x");
    Serial.println(MPU_ADDR, HEX);
  }
  else
  {
    Serial.println("Dispositivo no encontrado!");
  }
}

/*
 * Función que verifica si el sensor responde correctamente y si está activo
 */
void checkMPU(int mpu_addr)
{
  findMPU(MPU_ADDR);

  int data = readRegMPU(WHO_AM_I); // Register 117 – Who Am I - 0x75

  if (data == 104)
  {
    Serial.println("MPU6050 Dispositivo respondió OK! (104)");

    data = readRegMPU(PWR_MGMT_1); // Register 107 – Power Management 1-0x6B

    if (data == 64)
      Serial.println("MPU6050 en modo SLEEP! (64)");
    else
      Serial.println("MPU6050 en modo ACTIVE!");
  }
  else
    Serial.println("Verifique el dispositivo - ¡MPU6050 NO disponible!");
}

/*
 * Función para inicializar el sensor
 */
void initMPU()
{
  setSleepOff();
  setGyroScale();
  setAccelScale();
}

void setAccelScale()
{
  writeRegMPU(ACCEL_CONFIG, 0);
}

void setGyroScale()
{
  writeRegMPU(GYRO_CONFIG, 0);
}

/* 
 * Función para desactivar el modo de sueño del sensor y activarlo
 */
void setSleepOff()
{
  writeRegMPU(PWR_MGMT_1, 0); // Escribe 0 en el registro PWR_MGMT_1 (0x6B) para desactivar el modo de sueño
}



DHT_Unified dht(DHTPIN, DHTTYPE);

uint32_t delayMS;
// Setup

unsigned long startTime;
void setup()
{
  serialgps.begin(9600);
  Serial.begin(115200);
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(STASSID);
  pinMode(LED_PIN, OUTPUT);
  initI2C();
  initMPU();
  checkMPU(MPU_ADDR);


  /* Explicitly set the ESP32 to be a WiFi-client, otherwise, it by default,
     would try to act as both a client and an access-point and could cause
     network-issues with your other WiFi-devices on your WiFi-network. */
  WiFi.mode(WIFI_STA);
  WiFi.begin(STASSID, STAPSK);

  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }

 

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
  Serial.println("Setup!");

  dht.begin();
  Serial.println(F("DHTxx Unified Sensor"));



  // Init and get the time
  timeClient.begin();

 


  // Declarar la variable global para almacenar el valor del sensor de temperatura



}

String response;

String serializeSensorGpsStatesBody(int idSensorGps, char fechaHora[20], float valueLong, float valueLat)
{
  // StaticJsonObject allocates memory on the stack, it can be
  // replaced by DynamicJsonDocument which allocates in the heap.
  
  //
  DynamicJsonDocument doc(2048);

  // Add values in the document
  //
  doc["idSensorGps"] = idSensorGps;
  doc["fechaHora"] = fechaHora;
  doc["valueLong"] = valueLong;
  doc["valueLat"] = valueLat;
  doc["removed"] = false;

  // Generate the minified JSON and send it to the Serial port.
  //
  String output;
  serializeJson(doc, output);
  Serial.println(output);

  return output;
}

String serializeSensorGpsBody(int idDevice)
{
  // StaticJsonObject allocates memory on the stack, it can be
  // replaced by DynamicJsonDocument which allocates in the heap.
  
  //
  DynamicJsonDocument doc(2048);

  // Add values in the document
  //
  doc["idDevice"] = idDevice;
  doc["removed"] = false;

  // Generate the minified JSON and send it to the Serial port.
  //
  String output;
  serializeJson(doc, output);
  Serial.println(output);

  return output;
}


String serializeSensorACStatesBody(int idSensorAC, int valueAc, int valueGir)
{
  DynamicJsonDocument doc(2048);

  doc["idSensorAC"] = idSensorAC;
  doc["valueAc"] = valueAc;
  doc["valueGir"] = valueGir;
  doc["removed"] = false;

  String output;
  serializeJson(doc, output);
  return output;

}

String serializeSensorACBody(int idDevice)
{
  DynamicJsonDocument doc(2048);

  doc["idDevice"] = idDevice;
  doc["removed"] = false;

  String output;
  serializeJson(doc, output);
  return output;

}

String serializeDeviceBody(String deviceSerialId, String name)
{
  DynamicJsonDocument doc(2048);

  doc["deviceSerialId"] = deviceSerialId;
  doc["name"] = name;

  String output;
  serializeJson(doc, output);
  return output;
}

String serializeActuatorStatusBody(float status, bool statusBinary, int idActuator, long timestamp)
{
  DynamicJsonDocument doc(2048);

  doc["status"] = status;
  doc["statusBinary"] = statusBinary;
  doc["idActuator"] = idActuator;
  doc["timestamp"] = timestamp;
  doc["removed"] = false;

  String output;
  serializeJson(doc, output);
  return output;

}





void deserializeActuatorStatusBody(String responseJson)
{
  if (responseJson != "")
  {
    DynamicJsonDocument doc(2048);

    // Deserialize the JSON document
    DeserializationError error = deserializeJson(doc, responseJson);

    // Test if parsing succeeds.
    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    // Fetch values.
    int idActuatorState = doc["idActuatorState"];
    float status = doc["status"];
    bool statusBinary = doc["statusBinary"];
    int idActuator = doc["idActuator"];
    long timestamp = doc["timestamp"];

    Serial.println(("Actuator status deserialized: [idActuatorState: " + String(idActuatorState) + ", status: " + String(status) + ", statusBinary: " + String(statusBinary) + ", idActuator" + String(idActuator) + ", timestamp: " + String(timestamp) + "]").c_str());
  }
}

void deserializeSensorACStatesBody(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    DynamicJsonDocument doc(2048);

    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    int idSensorACStates = doc["idSensorACStates"];
    int idSensorAC = doc["idSensorAC"];
    int valueAc = doc["valueAc"];
    int valueGir = doc["valueGir"];
    

    Serial.println(("Device deserialized: [idSensorACStates: " + String(idSensorACStates) + ", idSensorAC: " + String(idSensorAC) +  ", valueAc: " + String(valueAc) + ", valueGir: " + String(valueGir) + "]").c_str());
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void deserializeSensorACBody(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    DynamicJsonDocument doc(2048);

    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    int idSensorAC = doc["idSensorAC"];
    int idDevice = doc["idDevice"];
   
    

    Serial.println(("Device deserialized: [idSensorAC: " + String(idSensorAC) + ", idDevice: " + String(idDevice) + "]").c_str());
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void deserializeSensorGpsStatesBody(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    DynamicJsonDocument doc(2048);

    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    int idSensorGpsStates = doc["idSensorGpsStates"];
    int idSensorGps = doc["idSensorGps"];
    unsigned long fechaHora = doc["fechaHora"];
    float valueLong = doc["valueLong"];
    float valueLat = doc["valueLat"];
    

    Serial.println(("Device deserialized: [idSensorGpsStates: " + String(idSensorGpsStates) + ", idSensorGps: " + String(idSensorGps) +  ", fechaHora: " + String(fechaHora) + ", valueLong: " + String(valueLong) + ", valueLat: " + String(valueLat) + "]").c_str());
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void deserializeSensorGpsBody(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    DynamicJsonDocument doc(2048);

    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    int idSensorGps = doc["idSensorGps"];
    int idDevice = doc["idDevice"];
    

    Serial.println(("Device deserialized: [idSensorAC: " + String(idSensorGps) + ", idDevice: " + String(idDevice) + "]").c_str());
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void deserializeDeviceBody(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    DynamicJsonDocument doc(2048);

    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    int idDevice = doc["idDevice"];
    String deviceSerialId = doc["deviceSerialId"];
    String name = doc["name"];
    

    Serial.println(("Device deserialized: [idDevice: " + String(idDevice) + ", name: " + name + ", deviceSerialId: " + deviceSerialId + "]").c_str());
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void deserializeSensorsAcStatesFromDevice(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    
    DynamicJsonDocument doc(ESP.getMaxAllocHeap());

    // parse a JSON array
    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    // extract the values
    JsonArray array = doc.as<JsonArray>();

    
    for (JsonObject sensor : array)
    {
      int idSensorACStates = sensor["idSensorACStates"];
      int valueAc = sensor["valueAc"];
      int valueGir = sensor["valueGir"];
      int idSensorAC = sensor["idSensorAC"];

      

      Serial.println(("Sensor deserialized: [idSensorACStates: " + String(idSensorACStates) + ", valueAc: " + String(valueAc) +  ", valueGir: " + String(valueGir) + ", idSensorAC: " + String(idSensorAC) + "]").c_str());
    }
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void deserializeSensorsGpsStatesFromDevice(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    
    DynamicJsonDocument doc(ESP.getMaxAllocHeap());

    // parse a JSON array
    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    // extract the values
    JsonArray array = doc.as<JsonArray>();

    
    for (JsonObject sensor : array)
    {
      int idSensorGpsStates = sensor["idSensorGpsStates"];
      unsigned long fechaHora = sensor["fechaHora"];
      float valueLong = sensor["valueLong"];
      float valueLat = sensor["valueLat"];
      int idSensorGps = sensor["idSensorGps"];


      

      Serial.println(("Sensor deserialized: [idSensorGpsStates: " + String(idSensorGpsStates) + ", fechaHora: " + String(fechaHora) +  ", valueLong: " + String(valueLong) + ", valueLat: " + String(valueLat) + ", idSensorGps: " + String(idSensorGps) + "]").c_str());
    }
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}



void deserializeActuatorsFromDevice(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    // allocate the memory for the document
    DynamicJsonDocument doc(ESP.getMaxAllocHeap());

    // parse a JSON array
    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    // extract the values
    JsonArray array = doc.as<JsonArray>();
    for (JsonObject sensor : array)
    {
      
      int idActuator = sensor["idActuator"];
      String name = sensor["name"];
      int idDevice = sensor["idDevice"];
      
      Serial.println(("Actuator deserialized: [idActuator: " + String(idActuator) + ", name: " + name + ", idDevice: " + String(idDevice) + "]").c_str());
  }
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }


}



void test_response(int httpResponseCode)
{
  delay(test_delay);
  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String payload = http.getString();
    Serial.println(payload);
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void describe(char *description)
{
  if (describe_tests)
    Serial.println(description);
}

void GET_tests()
{
  //describe("Test GET full device info");
  //String serverPath = serverName + "api/devices/" + String(DEVICE_ID);
  //http.begin(serverPath.c_str());
  //test_response(http.GET());
  //deserializeDeviceBody(http.GET());

  //describe("Test GET sensors from deviceID");
  //serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/sensors";
  //http.begin(serverPath.c_str());
  //deserializeSensorsFromDevice(http.GET());

  //describe("Test GET actuators from deviceID");
  //serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/actuators";
  //http.begin(serverPath.c_str());
  //deserializeActuatorsFromDevice(http.GET());

  //describe("Test GET sensors from deviceID and Type");
  //serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/sensors/Temperature";
  //http.begin(serverPath.c_str());
  //deserializeSensorsFromDevice(http.GET());

  //describe("Test GET actuators from deviceID");
  //serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/actuators/Buzzer";
  //http.begin(serverPath.c_str());
  //deserializeActuatorsFromDevice(http.GET());

//  describe("Test GET sensorsValues from SensorID");
//  String serverPath = serverName + "api/sensor_values/" + String(SENSOR_ID) + "/last";
//  http.begin(serverPath.c_str());
  
}

void POST_tests()
{
  sensors_event_t event;
  dht.temperature().getEvent(&event);
  float temperatura = event.temperature;


  char c = serialgps.read();
  gps.encode(c);
  latitude = gps.location.lat();
  longitude = gps.location.lng();
  snprintf(fechaHora, sizeof(fechaHora), "%04d-%02d-%02d %02d:%02d:%02d",
      gps.date.year(),
      gps.date.month(),
      gps.date.day(),
      gps.time.hour(),
      gps.time.minute(),
      gps.time.second()
  );
  //Obtener fechaHora Lat y Long ValorAC yvalorGir

  


  String actuator_states_body = serializeActuatorStatusBody(STATUS,STATUSBINARY,ACTUADOR_ID,millis());
  describe("Test POST with actuator state");
  String serverPath = serverName + "api/actuator_states";
  http.begin(serverPath.c_str());
  test_response(http.POST(actuator_states_body));

  String sensorGpsBody = serializeSensorGpsStatesBody(SENSOR_GPS_ID,fechaHora,latitude,longitude);
  describe("Test POST with actuator state");
  String serverPath = serverName + "api/actuator_states";
  http.begin(serverPath.c_str());
  test_response(http.POST(actuator_states_body));

  String sensorGpsBody = serializeSensorACStatesBody(SENSOR_AC_ID,ax, gy);
  describe("Test POST with actuator state");
  String serverPath = serverName + "api/actuator_states";
  http.begin(serverPath.c_str());
  test_response(http.POST(actuator_states_body));



}

void readRawMPU()
{
  Wire.beginTransmission(MPU_ADDR);
  Wire.write(ACCEL_XOUT);
  Wire.endTransmission(false);
  Wire.requestFrom(MPU_ADDR, 14);

  AcX = Wire.read() << 8 | Wire.read();
  AcY = Wire.read() << 8 | Wire.read();
  AcZ = Wire.read() << 8 | Wire.read();

  Tmp = Wire.read() << 8 | Wire.read();

  GyX = Wire.read() << 8 | Wire.read();
  GyY = Wire.read() << 8 | Wire.read();
  GyZ = Wire.read() << 8 | Wire.read();

  // Conversión a unidades físicas
  ax = AcX / 16384.0; // Aceleración en g
  ay = AcY / 16384.0;
  az = AcZ / 16384.0;
  
  gx = GyX / 131.0; // Velocidad angular en °/s
  gy = GyY / 131.0;
  gz = GyZ / 131.0;

  Serial.print("AcX = ");
  Serial.print(ax);
  Serial.print("g | AcY = ");
  Serial.print(ay);
  Serial.print("g | AcZ = ");
  Serial.print(az);
  Serial.print("g | Tmp = ");
  Serial.print(Tmp / 340.00 + 36.53);
  Serial.print("°C | GyX = ");
  Serial.print(gx);
  Serial.print("°/s | GyY = ");
  Serial.print(gy);
  Serial.print("°/s | GyZ = ");
  Serial.println(gz);

  // Ejemplo de uso en una condición

  delay(50);
}





// Variables para controlar el tiempo transcurrido
// Run the tests!
// Run the tests!



int repeticiones;
void loop()
{

if(repeticiones == 1000){

  char c = serialgps.read();
  gps.encode(c);

  readRawMPU();

  delay(test_delay);

  
    
  
if(gps.date.isValid() && gps.time.isValid() && (ax > 1.5 || gy > 100) ){

    STATUS = 1.0;
    STATUSBINARY = true;
    POST_tests();
    repeticiones = 0;
  }
    // Verificar si la temperatura supera el umbral predefinido

  
    
    
    
}else{
    repeticiones++;
}
//timeClient.update();

}

  
