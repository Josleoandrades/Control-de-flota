# DAD_backend

---

Objetivo: Encender un actuador cuando los valores de un sensor de temperatura supere cierto umbral, en este caso 30 grados.

## Clase MqttClientUtil

La clase `MqttClientUtil` es una clase utilitaria para interactuar con un cliente MQTT utilizando Vert.x. Proporciona métodos para publicar mensajes MQTT, suscribirse a temas MQTT y cancelar la suscripción a temas MQTT.

### Métodos

| Nombre                  | Descripción                                                                                                                                                               | Entrada                              | Salida                         |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------|--------------------------------|
| `publishMqttMessage`    | Publica un mensaje MQTT en el tema especificado.                                                                                                                          | `topic` (String): El tema MQTT.       | `handler` (Handler<AsyncResult<Integer>>): Un controlador de resultado que se invoca cuando se completa la publicación del mensaje. |
| `subscribeMqttTopic`    | Se suscribe a un tema MQTT para recibir mensajes.                                                                                                                          | `topic` (String): El tema MQTT.       | `handler` (Handler<AsyncResult<Integer>>): Un controlador de resultado que se invoca cuando se completa la suscripción al tema. |
| `unsubscribeMqttTopic`  | Cancela la suscripción a un tema MQTT.                                                                                                                                     | `topic` (String): El tema MQTT.       |                                  |
| `getInstance`           | Obtiene una instancia de la clase `MqttClientUtil`. Si no existe una instancia previa, se crea una nueva instancia utilizando el objeto `Vertx` especificado.          | `vertx` (Vertx): El objeto Vert.x.    | `MqttClientUtil`: La instancia de `MqttClientUtil`.              |

### Atributos

| Nombre             | Descripción                                                                                                                                                                           |
|--------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `mqttClient`       | El cliente MQTT utilizado para establecer y mantener una conexión con el broker MQTT.                                                                                                 |
| `mqttClientClass`  | La instancia única de la clase `MqttClientUtil`. Se utiliza el patrón Singleton para asegurar que solo haya una instancia de la clase en todo el programa.                           |

### Constructor

| Nombre                | Descripción                                                                                             | Entrada                     |
|-----------------------|---------------------------------------------------------------------------------------------------------|-----------------------------|
| `MqttClientUtil`      | Crea una nueva instancia de la clase `MqttClientUtil` y establece la conexión con el broker MQTT.       | `vertx` (Vertx): El objeto Vert.x utilizado para la creación del cliente MQTT. |

#### Descripción detallada:

El constructor `MqttClientUtil` crea una instancia de la clase `MqttClientUtil` y establece una conexión con el broker MQTT en el puerto 1883 en el host "localhost". Utiliza un objeto `MqttClientOptions` predeterminado para configurar el cliente MQTT. Una vez que se establece la conexión, se muestra un mensaje de éxito o se imprime un error en caso de que la conexión falle.

### Uso de la clase `MqttClientUtil`

Para utilizar la clase `MqttClientUtil`, se recomienda seguir los siguientes pasos:

1. Crear una instancia de `Vertx` utilizando el método `Vertx.vertx()`.
2. Obtener una instancia de `MqttClientUtil` utilizando el método estático `getInstance` y pasando el objeto `Vertx` creado en el paso anterior

.
3. Utilizar los métodos `publishMqttMessage`, `subscribeMqttTopic` y `unsubscribeMqttTopic` para interactuar con el cliente MQTT.

Ejemplo de uso:

```java
import io.vertx.core.Vertx;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public class Main {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        MqttClientUtil mqttClientUtil = MqttClientUtil.getInstance(vertx);

        mqttClientUtil.publishMqttMessage("topic", "payload", new Handler<AsyncResult<Integer>>() {
            @Override
            public void handle(AsyncResult<Integer> result) {
                if (result.succeeded()) {
                    System.out.println("Message published successfully");
                } else {
                    System.err.println("Failed to publish message: " + result.cause());
                }
            }
        });

        mqttClientUtil.subscribeMqttTopic("topic", new Handler<AsyncResult<Integer>>() {
            @Override
            public void handle(AsyncResult<Integer> result) {
                if (result.succeeded()) {
                    System.out.println("Subscribed to topic successfully");
                } else {
                    System.err.println("Failed to subscribe to topic: " + result.cause());
                }
            }
        });

        mqttClientUtil.unsubscribeMqttTopic("topic");
    }
}
```

**Nota**: Este código es solo un ejemplo y debe ajustarse según tus necesidades y entorno MQTT específico.

---
  
## Clase SensorValuesController

Esta clase es un controlador asociado a la entidad SensorValue. Realiza operaciones relacionadas con esta entidad a petición del verticle que despliega la API Rest.

### Constructor

|   Nombre    |     Descripción     |
|-------------|---------------------|
| SensorValuesController() | Constructor de la clase. Indica el tipo de entidad manejada por el controlador a la clase AbstractController. |

### Métodos

|   Nombre    |     Descripción     | Entrada | Salida |
|-------------|---------------------|---------|--------|
| start(Promise\<Void> startFuture) | Permite lanzar el verticle y desplegar el controlador que atenderá las solicitudes de la API Rest. | `startFuture`: Promesa que se completa cuando se ha iniciado el controlador. | N/A |
| stop(Future\<Void> stopFuture) | Detiene el controlador. | `stopFuture`: Futuro que se completa cuando se ha detenido el controlador. | N/A |

El método `start` implementa un switch para gestionar diferentes mensajes provenientes de la API Rest y delegarlos al método `launchDatabaseOperation` de la clase AbstractController para su procesamiento por el Verticle de acceso a datos.

Dentro del switch, los diferentes mensajes se gestionan de la siguiente manera:

|     Mensaje       |     Método     |   Descripción    | Entrada | Salida |
|-------------------|----------------|-----------------|---------|--------|
| CreateSensorValue | launchDatabaseOperation | Crea un SensorValue y realiza operaciones relacionadas, como obtener entidades relacionadas y publicar mensajes MQTT. | `message`: Mensaje de la API Rest. | N/A |
| DeleteSensorValue | launchDatabaseOperation | Elimina un SensorValue. | `message`: Mensaje de la API Rest. | N/A |
| GetLastSensorValueFromSensorId | launchDatabaseOperation | Obtiene el último SensorValue asociado a un SensorId específico. | `message`: Mensaje de la API Rest. | N/A |
| GetLatestSensorValuesFromSensorId | launchDatabaseOperation | Obtiene los últimos SensorValues asociados a un SensorId específico. | `message`: Mensaje de la API Rest. | N/A |
| Default Case | N/A | En caso de que el mensaje no pueda ser gestionado por este controlador, se envía una respuesta de error 401 a la API Rest. | `message`: Mensaje de la API Rest. | N/A |

  
#### Descripción detallada:

El constructor de la clase `SensorValuesController` es el encargado de inicializar una instancia de la clase. No recibe ningún parámetro y no genera ninguna salida. Su función principal es indicar el tipo de entidad manejada por el controlador a la clase `AbstractController`, donde se define la funcionalidad básica de los controladores.

### Ejemplo de uso de la clase SensorValuesController:

```java
import io.vertx.core.Promise;
import io.vertx.core.Vertx;

public class MainVerticle {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // Crear una instancia de SensorValuesController
        SensorValuesController sensorValuesController = new SensorValuesController();

        // Iniciar el controlador
        Promise<Void> startPromise = Promise.promise();
        sensorValuesController.start(startPromise);

        startPromise.future().onSuccess(v -> {
            System.out.println("SensorValuesController iniciado correctamente");
            // El controlador está listo para recibir solicitudes de la API Rest
        }).onFailure(err -> {
            System.err.println("Error al iniciar SensorValuesController: " + err.getMessage());
            // Ocurrió un error al iniciar el controlador
        });

        // Detener el controlador
        Promise<Void> stopPromise = Promise.promise();
        sensorValuesController.stop(stopPromise);

        stopPromise.future().onSuccess(v -> {
            System.out.println("SensorValuesController detenido correctamente");
            // El controlador se ha detenido correctamente
        }).onFailure(err -> {
            System.err.println("Error al detener SensorValuesController: " + err.getMessage());
            // Ocurrió un error al detener el controlador
        });
    }
}
```

En el ejemplo anterior, se muestra cómo crear una instancia de `SensorValuesController` y utilizarla en un programa Vert.x. Primero, se crea una instancia del objeto Vertx. Luego, se instancia un objeto `SensorValuesController`. A continuación, se inicia el controlador llamando al método `start()` y pasando una promesa `startPromise` que se completará cuando el controlador esté listo para recibir solicitudes de la API Rest. Después de iniciar el controlador, se puede realizar cualquier operación adicional necesaria antes de que el programa finalice.

Finalmente, se llama al método `stop()` del controlador para detenerlo. Al igual que con el método `start()`, se pasa una promesa `stopPromise` que se completará cuando el controlador se haya detenido correctamente. Esto permite realizar cualquier operación adicional antes de que el programa finalice por completo.

---

## Clase: Launcher
El `Launcher` es el punto de entrada y controlador principal del proyecto. Se encarga de lanzar todos los verticles necesarios para la ejecución del sistema.

### Métodos

| Nombre        | Propósito           | Parámetros de entrada  | Salida  |
| ------------- | ------------- | ------------- | ------------- |
| start()      | Inicia la ejecución del `Launcher` y despliega los verticles | startFuture: `Promise<Void>` - Promesa de finalización   | N/A |
| stop()      | Detiene la ejecución del `Launcher`     | stopFuture: `Future<Void>` - Futuro de finalización   | N/A |

#### Método: start()

Inicia la ejecución del `Launcher` y despliega los verticles necesarios. Se encarga de gestionar las promesas de finalización para asegurarse de que todos los verticles se desplieguen correctamente.

| Parámetro        | Descripción           |
| ------------- | ------------- |
| startFuture      | Promesa de finalización que se completará cuando todos los verticles se hayan desplegado correctamente.   |

#### Método: stop()

Detiene la ejecución del `Launcher`. Se invoca al detener el sistema.

| Parámetro        | Descripción           |
| ------------- | ------------- |
| stopFuture      | Futuro de finalización que se completará cuando el `Launcher` se haya detenido correctamente.   |

### Verticles

| Nombre        | Descripción           |
| ------------- | ------------- |
| DevicesController      | Controlador para la gestión de dispositivos.   |
| SensorsController      | Controlador para la gestión de sensores.   |
| ActuatorsController      | Controlador para la gestión de actuadores.   |
| GroupsController      | Controlador para la gestión de grupos.   |
| SensorValuesController      | Controlador para la gestión de valores de sensores.   |
| ActuatorStatesController      | Controlador para la gestión de estados de actuadores.   |
| MySQLVerticle      | Verticle para la gestión de la base de datos MySQL.   |
| RestAPIVerticle      | Verticle para la implementación de la API REST.   |

### Ejemplo de uso de la clase Launcher:


```java
public class MainApplication {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        Launcher launcher = new Launcher();

        vertx.deployVerticle(launcher, handler -> {
            if (handler.succeeded()) {
                System.out.println("Launcher started successfully");
            } else {
                System.out.println("Failed to start Launcher: " + handler.cause().getMessage());
            }
        });
    }
}
```

En el ejemplo anterior, se crea una instancia de la clase `Launcher` y se despliega como un verticle en un objeto `Vertx`. El método `deployVerticle()` se utiliza para desplegar el `Launcher`, y se proporciona un controlador para manejar la finalización del despliegue. Si el despliegue es exitoso, se muestra un mensaje indicando que el `Launcher` se inició correctamente. En caso de que falle, se muestra un mensaje de error con la causa del fallo.

---

