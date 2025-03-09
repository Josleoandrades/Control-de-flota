package es.us.dad.mysql.rest;

import java.util.Calendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import es.us.dad.mysql.entities.Actuator;
import es.us.dad.mysql.entities.ActuatorStatus;
import es.us.dad.mysql.entities.Device;
import es.us.dad.mysql.entities.Group;
import es.us.dad.mysql.entities.SensorAC;
import es.us.dad.mysql.entities.SensorGps;
import es.us.dad.mysql.entities.sensorACStates;
import es.us.dad.mysql.entities.sensorGpsStates;
import es.us.dad.mysql.entities.LatLong;
import es.us.dad.mqtt.MqttClientUtil;
//import es.us.dad.mysql.messages.DatabaseEntity;
//import es.us.dad.mysql.messages.DatabaseMessage;
//import es.us.dad.mysql.messages.DatabaseMessageIdAndActuatorType;
//import es.us.dad.mysql.messages.DatabaseMessageIdAndSensorType;
//import es.us.dad.mysql.messages.DatabaseMessageLatestValues;
//import es.us.dad.mysql.messages.DatabaseMessageType;
//import es.us.dad.mysql.messages.DatabaseMethod;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.sql.Timestamp;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class RestAPIVerticle extends AbstractVerticle {

	private transient Gson gson;
	MySQLPool mySqlClient;
	
	protected static transient MqttClient mqttClient;

	private static transient MqttClientUtil mqttClientClass = null;

	@Override
	public void start(Promise<Void> startFuture) {
		
//		mqttClient = MqttClient.create(vertx, new MqttClientOptions());
//		mqttClient.connect(1883, "172.20.10.2", s -> {
//			if (s.succeeded()) {
//				System.out.println("Sucessfully connected to MQTT brocker");
//			} else {
//				System.err.println(s.cause());
//			}
//		});
		
		MySQLConnectOptions connectOptions = new MySQLConnectOptions().setPort(3306).setHost("localhost")
				.setDatabase("wait").setUser("root").setPassword("Jo5ejr12J");

		PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

		mySqlClient = MySQLPool.pool(vertx, connectOptions, poolOptions);

		// Instantiating a Gson serialize object using specific date format
		gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();

		// Defining the router object
		Router router = Router.router(vertx);

		// Handling any server startup result
		HttpServer httpServer = vertx.createHttpServer();
		httpServer.requestHandler(router::handle).listen(8080, result -> {
			if (result.succeeded()) {
				System.out.println("API Rest is listening on port 8080");
				startFuture.complete();
			} else {
				startFuture.fail(result.cause());
			}
		});

		// Defining URI paths for each method in RESTful interface, including body
		// handling
		router.route("/api*").handler(BodyHandler.create());

		// Endpoint definition for CRUD ops
		
		//SensorsAC
		router.get("/api/sensorsAc/:sensorAc").handler(this::getSensorAcById); //OK
		router.post("/api/sensorsAc").handler(this::addSensorAc); //OK
		router.delete("/api/sensorsAc/:sensorAcid").handler(this::deleteSensorAc); //OK
		//router.put("/api/sensorsAc/:sensorAcid").handler(this::putSensorAc); //OK
		
		//SesnorACStates
		router.get("/api/sensorsAcStates/:sensorAcState").handler(this::getSensorAcStateById); //OK
		router.post("/api/sensorsAcStates").handler(this::addSensorAcState); //OK
		router.delete("/api/sensorsAcStates/:sensorAcStatesid").handler(this::deleteSensorAcState); //OK
		//router.put("/api/sensorsAc/:sensorAcid").handler(this::putSensorAc); //OK
		
		//SensorsGps
		router.get("/api/sensorsGps/:sensorGps").handler(this::getSensorGpsById); //OK
		router.post("/api/sensorsGps").handler(this::addSensorGps); //OK
		router.delete("/api/sensorsGps/:sensorGpsid").handler(this::deleteSensorGps); //OK
		//router.put("/api/sensorsGps/:sensorGpsid").handler(this::putSensor); //OK
		
		//SensorsGpsStates
		router.get("/api/sensorsGpsStates/:sensorGpsStates").handler(this::getSensorGpsStatesById); //OK
		router.post("/api/sensorsGpsStates").handler(this::addSensorGpsStates); //OK
		router.delete("/api/sensorsGpsStates/:sensorGpsStatesid").handler(this::deleteSensorGpsStates); //OK
		//router.put("/api/sensorsGps/:sensorGpsid").handler(this::putSensor); //OK

		
		//Devices
		router.get("/api/devices/:device").handler(this::getDeviceById); //OK
		router.post("/api/devices").handler(this::addDevice); //OK
		router.delete("/api/devices/:deviceid").handler(this::deleteDevice); //OK
		//router.put("/api/devices/:deviceid").handler(this::putDevice); //OK
		router.get("/api/devices/:deviceid/sensorsAcStates").handler(this::getSensorsAcStatesFromDevice); //OK
		router.get("/api/devices/:deviceid/sensorsGpsStates").handler(this::getSensorsGpsStatesFromDevice);
//		router.get("/api/devices/:deviceid/sensorsAcValue").handler(this::getSensorsAcValuesACFromDevice); //OK
//		router.get("/api/devices/:deviceid/sensorsGirValue").handler(this::getSensorsAcValuesGirFromDevice); //OK
//		router.get("/api/devices/:deviceid/sensorsGpsValues").handler(this::getSensorsGpsValuesFromDevice); //OK
//		router.get("/api/devices/:deviceid/sensorsGpsDate").handler(this::getSensorsGpsDateFromDevice); //OK
		router.get("/api/devices/:deviceid/actuators").handler(this::getActuatorsFromDevice); //OK
		
		
		
		
		//Actuadores
		router.get("/api/actuators/:actuator").handler(this::getActuatorById); //OK
		router.post("/api/actuators").handler(this::addActuator); //OK
		router.delete("/api/actuators/:actuatorid").handler(this::deleteActuator); //OK
		//router.put("/api/actuators/:actuatorid").handler(this::putActuator); //OK
		
		
		//ActuadorValues
		router.post("/api/actuator_states").handler(this::addActuatorStatus); //OK
		router.delete("/api/actuator_states/:actuatorstatusid").handler(this::deleteActuatorStatus); //OK
		router.get("/api/actuator_states/:actuatorid/last").handler(this::getLastActuatorStatus); //OK
	}


	//SensorAc
    
    private void getSensorAcById(RoutingContext routingContext) {
        mySqlClient.getConnection(connection -> {
            int idSensorAC = Integer.parseInt(routingContext.request().getParam("sensorAC")); // Accedemos al parámetro "id" en lugar de "sensor"
            if (connection.succeeded()) {
                connection.result().preparedQuery("SELECT * FROM wait.sensorsac WHERE idSensorsAC = ?;", Tuple.of(idSensorAC), res -> {
                    if (res.succeeded()) {
                        // Get the result set
                        RowSet<Row> resultSet = res.result();
                        System.out.println(resultSet.size());
                        List<SensorAC> result = new ArrayList<>();
                        for (Row elem : resultSet) {
                        	result.add(new SensorAC(elem.getInteger("idDevice"),
									 elem.getBoolean("removed")));
                        }
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .setStatusCode(200)
                                .end(gson.toJson(result));
                    } else {
                        System.out.println("Error: " + res.cause().getLocalizedMessage());
                        routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
                    }
                    // Cerramos la conexión después de obtener los resultados
                    connection.result().close();
                });
            } else {
                System.out.println(connection.cause().toString());
                routingContext.response().setStatusCode(500).end("Error con la conexión: " + connection.cause().getMessage());
            }
        });
    }
    
   
    
    
    protected void addSensorAc(RoutingContext routingContext) {
    	
    	 final SensorAC sensorAC = gson.fromJson(routingContext.getBodyAsString(),
		    		SensorAC.class);
    	 
    	 mySqlClient.preparedQuery(
 				"INSERT INTO wait.sensorsac (idDevice, removed) VALUES (?,?);",
 				Tuple.of(sensorAC.getIdDevice(), sensorAC.isRemoved()),
 				res -> {
 					if (res.succeeded()) {
 						//String topic =  sensorAC.getIdDevice().toString();
 	                    //String payload = gson.toJson(sensorAC);
 	                    //mqttClient.publish(topic, Buffer.buffer(payload), MqttQoS.AT_LEAST_ONCE, false, false);
						long lastInsertId = res.result().property(MySQLClient.LAST_INSERTED_ID);
						sensorAC.setIdSensorAC((int) lastInsertId);
						routingContext.response().setStatusCode(201).putHeader("content-type",
                                "application/json; charset=utf-8").end("Sensor añadido correctamente");
					} else {
						System.out.println("Error: " + res.cause().getLocalizedMessage());
                        routingContext.response().setStatusCode(500).end("Error al añadir el sensor: " + res.cause().getMessage());
					}
				});
	}
    
    protected void deleteSensorAc(RoutingContext routingContext) {
    	
   	 int idSensorAc = Integer.parseInt(routingContext.request().getParam("sensorAcid"));
		
		mySqlClient.preparedQuery("DELETE FROM wait.sensorsac WHERE idSensorsAC = ?;", Tuple.of(idSensorAc), res -> {
			if (res.succeeded()) {
				 if (res.result().rowCount() > 0) {
                    routingContext.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(gson.toJson(new JsonObject().put("message", "Sensor eliminado correctamente")));
                } 
			} else {
				System.out.println("Error: " + res.cause().getLocalizedMessage());
	            routingContext.response()
	                    .setStatusCode(500)
	                    .end("Error al conectar con la base de datos: ");
			}
		});
	}
    
    //SensorACStates
    
    private void getSensorAcStateById(RoutingContext routingContext) {
        mySqlClient.getConnection(connection -> {
            int idSensorACStates = Integer.parseInt(routingContext.request().getParam("sensorAcState")); // Accedemos al parámetro "id" en lugar de "sensor"
            if (connection.succeeded()) {
                connection.result().preparedQuery("SELECT * FROM wait.sensorsacstates WHERE idSensorsACStates = ?;", Tuple.of(idSensorACStates), res -> {
                    if (res.succeeded()) {
                        // Get the result set
                        RowSet<Row> resultSet = res.result();
                        System.out.println(resultSet.size());
                        List<sensorACStates> result = new ArrayList<>();
                        for (Row elem : resultSet) {
                        	result.add(new sensorACStates(elem.getInteger("idSensorAC"), elem.getInteger("valueAc"), elem.getInteger("valueGir"),
									 elem.getBoolean("removed")));
                        }
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .setStatusCode(200)
                                .end(gson.toJson(result));
                    } else {
                        System.out.println("Error: " + res.cause().getLocalizedMessage());
                        routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
                    }
                    // Cerramos la conexión después de obtener los resultados
                    connection.result().close();
                });
            } else {
                System.out.println(connection.cause().toString());
                routingContext.response().setStatusCode(500).end("Error con la conexión: " + connection.cause().getMessage());
            }
        });
    }
    
    protected void addSensorAcState(RoutingContext routingContext) {
    	
   	 final sensorACStates sensorACStates = gson.fromJson(routingContext.getBodyAsString(),
		    		sensorACStates.class);
   	 
   	 mySqlClient.preparedQuery(
				"INSERT INTO wait.sensorsacstates (idSensorAC, valueAc, ValueGir, removed) VALUES (?,?,?,?);",
				Tuple.of(sensorACStates.getIdSensorAC(), sensorACStates.getValueAc(), sensorACStates.getValueGir(), sensorACStates.isRemoved()),
				res -> {
					if (res.succeeded()) {
						//String topic =  sensorAC.getIdDevice().toString();
	                    //String payload = gson.toJson(sensorAC);
	                    //mqttClient.publish(topic, Buffer.buffer(payload), MqttQoS.AT_LEAST_ONCE, false, false);
						long lastInsertId = res.result().property(MySQLClient.LAST_INSERTED_ID);
						sensorACStates.setIdSensorAC((int) lastInsertId);
						routingContext.response().setStatusCode(201).putHeader("content-type",
                               "application/json; charset=utf-8").end("Sensor añadido correctamente");
					} else {
						System.out.println("Error: " + res.cause().getLocalizedMessage());
                       routingContext.response().setStatusCode(500).end("Error al añadir el sensor: " + res.cause().getMessage());
					}
				});
	}
    
    protected void deleteSensorAcState(RoutingContext routingContext) {
    	
   	 int idSensorAcStates = Integer.parseInt(routingContext.request().getParam("sensorAcStatesid"));
		
		mySqlClient.preparedQuery("DELETE FROM wait.sensorsacstates WHERE idSensorsACStates = ?;", Tuple.of(idSensorAcStates), res -> {
			if (res.succeeded()) {
				 if (res.result().rowCount() > 0) {
                    routingContext.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(gson.toJson(new JsonObject().put("message", "Sensor eliminado correctamente")));
                } 
			} else {
				System.out.println("Error: " + res.cause().getLocalizedMessage());
	            routingContext.response()
	                    .setStatusCode(500)
	                    .end("Error al conectar con la base de datos: ");
			}
		});
	}
    
    //SensorGps
    
    private void getSensorGpsById(RoutingContext routingContext) {
        mySqlClient.getConnection(connection -> {
            int idSensorGps = Integer.parseInt(routingContext.request().getParam("sensorGps")); // Accedemos al parámetro "id" en lugar de "sensor"
            if (connection.succeeded()) {
                connection.result().preparedQuery("SELECT * FROM wait.sensorsgps WHERE idSensorsGps = ?;", Tuple.of(idSensorGps), res -> {
                    if (res.succeeded()) {
                        // Get the result set
                        RowSet<Row> resultSet = res.result();
                        System.out.println(resultSet.size());
                        List<SensorGps> result = new ArrayList<>();
                        for (Row elem : resultSet) {
                        	result.add(new SensorGps(elem.getInteger("idDevice"),
									 elem.getBoolean("removed")));
                        }
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .setStatusCode(200)
                                .end(gson.toJson(result));
                    } else {
                        System.out.println("Error: " + res.cause().getLocalizedMessage());
                        routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
                    }
                    // Cerramos la conexión después de obtener los resultados
                    connection.result().close();
                });
            } else {
                System.out.println(connection.cause().toString());
                routingContext.response().setStatusCode(500).end("Error con la conexión: " + connection.cause().getMessage());
            }
        });
    }
    
    protected void addSensorGps(RoutingContext routingContext) {
    	
   	 final SensorGps sensorGps = gson.fromJson(routingContext.getBodyAsString(),
   			SensorGps.class);
   	 
   	 mySqlClient.preparedQuery(
				"INSERT INTO wait.sensorsgps (idDevice, removed) VALUES (?,?);",
				Tuple.of(sensorGps.getIdDevice(), sensorGps.isRemoved()),
				res -> {
					if (res.succeeded()) {
						//String topic =  sensorAC.getIdDevice().toString();
	                    //String payload = gson.toJson(sensorAC);
	                    //mqttClient.publish(topic, Buffer.buffer(payload), MqttQoS.AT_LEAST_ONCE, false, false);
						long lastInsertId = res.result().property(MySQLClient.LAST_INSERTED_ID);
						sensorGps.setIdSensorGps((int) lastInsertId);
						routingContext.response().setStatusCode(201).putHeader("content-type",
                               "application/json; charset=utf-8").end("Sensor añadido correctamente");
					} else {
						System.out.println("Error: " + res.cause().getLocalizedMessage());
                       routingContext.response().setStatusCode(500).end("Error al añadir el sensor: " + res.cause().getMessage());
					}
				});
	}
    
 
    
    
//    protected void putSensor(RoutingContext routingContext) {
//    	
//    	
//    	
//    	final Sensor sensor = gson.fromJson(routingContext.getBodyAsString(), Sensor.class);
//    	
//		mySqlClient.preparedQuery(
//				"UPDATE dad.sensors g SET name = COALESCE(?, g.name), idDevice = COALESCE(?, g.idDevice), sensorType = COALESCE(?, g.sensorType), removed = COALESCE(?, g.removed) WHERE idSensor = ?;",
//				Tuple.of(sensor.getName(), sensor.getIdDevice(), sensor.getSensorType(), sensor.isRemoved(),
//						sensor.getIdSensor()),
//				res -> {
//					if (res.succeeded()) {
//						if (res.result().rowCount() > 0) {
//	                        routingContext.response()
//	                            .setStatusCode(200)
//	                            .putHeader("content-type", "application/json; charset=utf-8")
//	                            .end(gson.toJson(sensor));
//	                    } 
//					} else {
//						System.out.println("Error: " + res.cause().getLocalizedMessage());
//		  	              routingContext.response()
//		  	                .putHeader("content-type", "application/json; charset=utf-8")
//		  	                        .setStatusCode(404)
//		  	                        .end("Error al actualizar los sensores: " + res.cause().getMessage());
//					}
//				});
//		
//		
//		
//	}
    
    
    
    protected void deleteSensorGps(RoutingContext routingContext) {
    	
   	 int idSensorGps = Integer.parseInt(routingContext.request().getParam("sensorGpsid"));
		
		mySqlClient.preparedQuery("DELETE FROM wait.sensorsgps WHERE idSensorsGps = ?;", Tuple.of(idSensorGps), res -> {
			if (res.succeeded()) {
				 if (res.result().rowCount() > 0) {
                    routingContext.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(gson.toJson(new JsonObject().put("message", "Sensor eliminado correctamente")));
                } 
			} else {
				System.out.println("Error: " + res.cause().getLocalizedMessage());
	            routingContext.response()
	                    .setStatusCode(500)
	                    .end("Error al conectar con la base de datos: ");
			}
		});
	}
    
    //SensorGpsStates
    
    private void getSensorGpsStatesById(RoutingContext routingContext) {
        mySqlClient.getConnection(connection -> {
            int idSensorGpsStates = Integer.parseInt(routingContext.request().getParam("sensorGpsStates")); // Accedemos al parámetro "id" en lugar de "sensor"
            if (connection.succeeded()) {
                connection.result().preparedQuery("SELECT * FROM wait.sensorsgpsstates WHERE idSensorsGpsStates = ?;", Tuple.of(idSensorGpsStates), res -> {
                    if (res.succeeded()) {
                        // Get the result set
                        RowSet<Row> resultSet = res.result();
                        System.out.println(resultSet.size());
                        List<sensorGpsStates> result = new ArrayList<>();
                        for (Row elem : resultSet) {
                        	result.add(new sensorGpsStates(elem.getInteger("idSensorGps"), Timestamp.valueOf(elem.getLocalDateTime("fechaHora")), elem.getFloat("valueLong"), elem.getFloat("valueLat"),
									 elem.getBoolean("removed")));
                        }
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .setStatusCode(200)
                                .end(gson.toJson(result));
                    } else {
                        System.out.println("Error: " + res.cause().getLocalizedMessage());
                        routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
                    }
                    // Cerramos la conexión después de obtener los resultados
                    connection.result().close();
                });
            } else {
                System.out.println(connection.cause().toString());
                routingContext.response().setStatusCode(500).end("Error con la conexión: " + connection.cause().getMessage());
            }
        });
    }
    
    protected void addSensorGpsStates(RoutingContext routingContext) {
    	
      	 final sensorGpsStates sensorGpsStates = gson.fromJson(routingContext.getBodyAsString(),
      			sensorGpsStates.class);
      	 
      	 mySqlClient.preparedQuery(
   				"INSERT INTO wait.sensorsgpsstates (idSensorGps, fechaHora, valueLong, valueLat removed) VALUES (?,?,?,?,?);",
   				Tuple.of(sensorGpsStates.getIdSensorGps(), sensorGpsStates.getFechaHora(), sensorGpsStates.getValueLong(), sensorGpsStates.getValueLat(), sensorGpsStates.isRemoved()),
   				res -> {
   					if (res.succeeded()) {
   						//String topic =  sensorAC.getIdDevice().toString();
   	                    //String payload = gson.toJson(sensorAC);
   	                    //mqttClient.publish(topic, Buffer.buffer(payload), MqttQoS.AT_LEAST_ONCE, false, false);
   						long lastInsertId = res.result().property(MySQLClient.LAST_INSERTED_ID);
   						sensorGpsStates.setIdSensorGps((int) lastInsertId);
   						routingContext.response().setStatusCode(201).putHeader("content-type",
                                  "application/json; charset=utf-8").end("Sensor añadido correctamente");
   					} else {
   						System.out.println("Error: " + res.cause().getLocalizedMessage());
                          routingContext.response().setStatusCode(500).end("Error al añadir el sensor: " + res.cause().getMessage());
   					}
   				});
   	}
    
    protected void deleteSensorGpsStates(RoutingContext routingContext) {
    	
      	 int idsensorGpsStates = Integer.parseInt(routingContext.request().getParam("sensorGpsStatesid"));
   		
   		mySqlClient.preparedQuery("DELETE FROM wait.sensorsgpsstates WHERE idSensorsGpsStates = ?;", Tuple.of(idsensorGpsStates), res -> {
   			if (res.succeeded()) {
   				 if (res.result().rowCount() > 0) {
                       routingContext.response()
                           .setStatusCode(200)
                           .putHeader("content-type", "application/json; charset=utf-8")
                           .end(gson.toJson(new JsonObject().put("message", "Sensor eliminado correctamente")));
                   } 
   			} else {
   				System.out.println("Error: " + res.cause().getLocalizedMessage());
   	            routingContext.response()
   	                    .setStatusCode(500)
   	                    .end("Error al conectar con la base de datos: ");
   			}
   		});
   	}
   
	
	//Devices
	
 
    
	
	protected void getDeviceById(RoutingContext routingContext) {
		int deviceId = Integer.parseInt(routingContext.request().getParam("device"));
		mySqlClient.preparedQuery("SELECT * FROM wait.devices WHERE idDevice = ?;", Tuple.of(deviceId), res -> {
			 if (res.succeeded()) {
                 // Get the result set
                 RowSet<Row> resultSet = res.result();
                 System.out.println(resultSet.size());
                 List<Device> result = new ArrayList<>();
                 for (Row elem : resultSet) {
                 	result.add(new Device(elem.getInteger("idDevice"), elem.getString("deviceSerialId"),
							elem.getString("name"),
							elem.getLong("lastTimestampSensorModified"), elem.getLong("lastTimestampActuatorModified")));
                 }
                 routingContext.response()
                         .putHeader("content-type", "application/json; charset=utf-8")
                         .setStatusCode(200)
                         .end(gson.toJson(result));
             } else {
                 System.out.println("Error: " + res.cause().getLocalizedMessage());
                 routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
             }
		});
	}
	
	protected void addDevice(RoutingContext routingContext) {
		final Device device = gson.fromJson(routingContext.getBodyAsString(), Device.class);
		mySqlClient.preparedQuery(
				"INSERT INTO wait.devices (deviceSerialId, name,  lastTimestampSensorModified,"
						+ " lastTimestampActuatorModified) VALUES (?,?,?,?);",
				Tuple.of(device.getDeviceSerialId(), device.getName(),
						device.getLastTimestampSensorModified(), device.getLastTimestampActuatorModified()),
				res -> {
					if (res.succeeded()) {
						long lastInsertId = res.result().property(MySQLClient.LAST_INSERTED_ID);
						device.setIdDevice((int) lastInsertId);
						routingContext.response().setStatusCode(201).putHeader("content-type",
                                "application/json; charset=utf-8").end("Actuador añadido correctamente");
					} else {
						System.out.println("Error: " + res.cause().getLocalizedMessage());
                        routingContext.response().setStatusCode(500).end("Error al añadir el sensor: " + res.cause().getMessage());
					}
				});
	}
	
	protected void deleteDevice(RoutingContext routingContext) {
		int deviceId = Integer.parseInt(routingContext.request().getParam("deviceid"));
		mySqlClient.preparedQuery("DELETE FROM wait.devices WHERE idDevice = ?;", Tuple.of(deviceId), res -> {
			if (res.succeeded()) {
				if (res.result().rowCount() > 0) {
                    routingContext.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(gson.toJson(new JsonObject().put("message", "Dispositivo eliminado correctamente")));
                } 
			} else {
				System.out.println("Error: " + res.cause().getLocalizedMessage());
	            routingContext.response()
	                    .setStatusCode(500)
	                    .end("Error al conectar con la base de datos: ");
			}
		});
	}
	
	protected void putDevice(RoutingContext routingContext) {
		final Device device = gson.fromJson(routingContext.getBodyAsString(), Device.class);
		mySqlClient.preparedQuery(
				"UPDATE wait.devices g SET deviceSerialId = COALESCE(?, g.deviceSerialId), name = COALESCE(?, g.name), lastTimestampSensorModified = COALESCE(?, g.lastTimestampSensorModified), lastTimestampActuatorModified = COALESCE(?, g.lastTimestampActuatorModified) WHERE idDevice = ?;",
				Tuple.of(device.getDeviceSerialId(), device.getName(),
						device.getLastTimestampSensorModified(), device.getLastTimestampActuatorModified(),
						device.getIdDevice()),
				res -> {
					if (res.succeeded()) {
                        routingContext.response()
                            .setStatusCode(200)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(gson.toJson(device));
                    } else {
					System.out.println("Error: " + res.cause().getLocalizedMessage());
	  	              routingContext.response()
	  	                .putHeader("content-type", "application/json; charset=utf-8")
	  	                        .setStatusCode(404)
	  	                        .end("Error al actualizar los sensores: " + res.cause().getMessage());
				}
				});
	}




	
//	protected void getSensorsFromDevice(RoutingContext routingContext) {
//		int deviceId = Integer.parseInt(routingContext.request().getParam("deviceid"));
//		mySqlClient.preparedQuery("SELECT * FROM wait.sensors WHERE idDevice = ?;", Tuple.of(deviceId), res -> {
//			if (res.succeeded()) {
//				// Get the result set
//				RowSet<Row> resultSet = res.result();
//				List<Sensor> result = new ArrayList<>();
//				for (Row elem : resultSet) {
//					result.add(new Sensor(elem.getInteger("idSensor"), elem.getString("name"),
//							elem.getInteger("idDevice"), elem.getString("sensorType"), elem.getBoolean("removed")));
//				}
//
//				 routingContext.response()
//                 .putHeader("content-type", "application/json; charset=utf-8")
//                 .setStatusCode(200)
//                 .end(gson.toJson(result));
//     } else {
//         System.out.println("Error: " + res.cause().getLocalizedMessage());
//         routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
//     }
//		});
//	}
	
	protected void getSensorsAcStatesFromDevice(RoutingContext routingContext) {
		int deviceId = Integer.parseInt(routingContext.request().getParam("deviceid"));
		mySqlClient.preparedQuery("SELECT * FROM wait.sensorsacstates WHERE idDevice = ?;", Tuple.of(deviceId),
				res -> {
					if (res.succeeded()) {
						// Get the result set
						RowSet<Row> resultSet = res.result();
						List<sensorACStates> result = new ArrayList<>();
						for (Row elem : resultSet) {
							result.add(new sensorACStates(elem.getInteger("idSensorACStates"), elem.getInteger("idSensorAC"), elem.getInteger("valueAc"), elem.getInteger("valueGir"), 
									
									elem.getBoolean("removed")));
						}

						routingContext.response()
		                 .putHeader("content-type", "application/json; charset=utf-8")
		                 .setStatusCode(200)
		                 .end(gson.toJson(result));
		     } else {
		         System.out.println("Error: " + res.cause().getLocalizedMessage());
		         routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
		     }
				});
	}
	
	protected void getSensorsGpsStatesFromDevice(RoutingContext routingContext) {
		int deviceId = Integer.parseInt(routingContext.request().getParam("deviceid"));
		mySqlClient.preparedQuery("SELECT * FROM wait.sensorsgps WHERE idDevice = ?;", Tuple.of(deviceId),
				res -> {
					if (res.succeeded()) {
						// Get the result set
						RowSet<Row> resultSet = res.result();
						List<sensorGpsStates> result = new ArrayList<>();
						for (Row elem : resultSet) {
							result.add(new sensorGpsStates(elem.getInteger("idSensorGps"), Timestamp.valueOf(elem.getLocalDateTime("fechaHora")), elem.getFloat("valueLong"), elem.getFloat("valueLat"),
									 elem.getBoolean("removed")));
						}

						routingContext.response()
		                 .putHeader("content-type", "application/json; charset=utf-8")
		                 .setStatusCode(200)
		                 .end(gson.toJson(result));
		     } else {
		         System.out.println("Error: " + res.cause().getLocalizedMessage());
		         routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
		     }
				});
	}
	
//	protected void getSensorsAcValuesACFromDevice(RoutingContext routingContext) {
//		int deviceId = Integer.parseInt(routingContext.request().getParam("deviceid"));
//		mySqlClient.preparedQuery("SELECT * FROM wait.sensorsac WHERE idDevice = ?;", Tuple.of(deviceId),
//				res -> {
//					if (res.succeeded()) {
//						// Get the result set
//						RowSet<Row> resultSet = res.result();
//						List<Integer> result = new ArrayList<>();
//						for (Row elem : resultSet) {
//							result.add(elem.getInteger("valueAc"));
//						}
//
//						routingContext.response()
//		                 .putHeader("content-type", "application/json; charset=utf-8")
//		                 .setStatusCode(200)
//		                 .end(gson.toJson(result));
//		     } else {
//		         System.out.println("Error: " + res.cause().getLocalizedMessage());
//		         routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
//		     }
//				});
//	}
//	
//	protected void getSensorsAcValuesGirFromDevice(RoutingContext routingContext) {
//		int deviceId = Integer.parseInt(routingContext.request().getParam("deviceid"));
//		mySqlClient.preparedQuery("SELECT * FROM wait.sensorsac WHERE idDevice = ?;", Tuple.of(deviceId),
//				res -> {
//					if (res.succeeded()) {
//						// Get the result set
//						RowSet<Row> resultSet = res.result();
//						List<Integer> result = new ArrayList<>();
//						for (Row elem : resultSet) {
//							result.add(elem.getInteger("valueGir"));
//						}
//
//						routingContext.response()
//		                 .putHeader("content-type", "application/json; charset=utf-8")
//		                 .setStatusCode(200)
//		                 .end(gson.toJson(result));
//		     } else {
//		         System.out.println("Error: " + res.cause().getLocalizedMessage());
//		         routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
//		     }
//				});
//	}
//	
//	protected void getSensorsGpsValuesFromDevice(RoutingContext routingContext) {
//		int deviceId = Integer.parseInt(routingContext.request().getParam("deviceid"));
//		mySqlClient.preparedQuery("SELECT * FROM wait.sensorsgps WHERE idDevice = ?;", Tuple.of(deviceId),
//				res -> {
//					if (res.succeeded()) {
//						// Get the result set
//						RowSet<Row> resultSet = res.result();
//						List<LatLong> result = new ArrayList<>();
//						for (Row elem : resultSet) {
//							result.add(new LatLong(elem.getFloat("valueLong"), elem.getFloat("valueLat")));
//						}
//
//						routingContext.response()
//		                 .putHeader("content-type", "application/json; charset=utf-8")
//		                 .setStatusCode(200)
//		                 .end(gson.toJson(result));
//		     } else {
//		         System.out.println("Error: " + res.cause().getLocalizedMessage());
//		         routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
//		     }
//				});
//	}
//	
//	protected void getSensorsGpsDateFromDevice(RoutingContext routingContext) {
//		int deviceId = Integer.parseInt(routingContext.request().getParam("deviceid"));
//		mySqlClient.preparedQuery("SELECT * FROM wait.sensorsgps WHERE idDevice = ?;", Tuple.of(deviceId),
//				res -> {
//					if (res.succeeded()) {
//						// Get the result set
//						RowSet<Row> resultSet = res.result();
//						List<Timestamp> result = new ArrayList<>();
//						for (Row elem : resultSet) {
//							result.add(Timestamp.valueOf(elem.getLocalDateTime("fechaHora")));
//						}
//
//						routingContext.response()
//		                 .putHeader("content-type", "application/json; charset=utf-8")
//		                 .setStatusCode(200)
//		                 .end(gson.toJson(result));
//		     } else {
//		         System.out.println("Error: " + res.cause().getLocalizedMessage());
//		         routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
//		     }
//				});
//	}
	
	
	protected void getActuatorsFromDevice(RoutingContext routingContext) {
		int deviceId = Integer.parseInt(routingContext.request().getParam("deviceid"));
		mySqlClient.preparedQuery("SELECT * FROM wait.actuators WHERE idDevice = ?;", Tuple.of(deviceId),
				res -> {
					if (res.succeeded()) {
						// Get the result set
						RowSet<Row> resultSet = res.result();
						List<Actuator> result = new ArrayList<>();
						for (Row elem : resultSet) {
							result.add(new Actuator(elem.getInteger("idActuator"), elem.getString("name"),
									elem.getInteger("idDevice"), elem.getString("actuatorType"),
									elem.getBoolean("removed")));
						}

						routingContext.response()
		                 .putHeader("content-type", "application/json; charset=utf-8")
		                 .setStatusCode(200)
		                 .end(gson.toJson(result));
		     } else {
		         System.out.println("Error: " + res.cause().getLocalizedMessage());
		         routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
		     }
				});
	}
	

//	protected void getSensorsFromDeviceAndType(RoutingContext routingContext) {
//		int deviceId = Integer.parseInt(routingContext.request().getParam("deviceid"));
//		SensorType type = SensorType.valueOf(routingContext.request().getParam("type"));
//		mySqlClient.preparedQuery("SELECT * FROM wait.sensors WHERE idDevice = ? AND sensorType = ?;",
//				Tuple.of(deviceId, type), res -> {
//					if (res.succeeded()) {
//						// Get the result set
//						RowSet<Row> resultSet = res.result();
//						List<Sensor> result = new ArrayList<>();
//						for (Row elem : resultSet) {
//							result.add(new Sensor(elem.getInteger("idSensor"), elem.getString("name"),
//									elem.getInteger("idDevice"), elem.getString("sensorType"),
//									elem.getBoolean("removed")));
//						}
//
//						routingContext.response()
//		                 .putHeader("content-type", "application/json; charset=utf-8")
//		                 .setStatusCode(200)
//		                 .end(gson.toJson(result));
//		     } else {
//		         System.out.println("Error: " + res.cause().getLocalizedMessage());
//		         routingContext.response().setStatusCode(500).end("Error al obtener los sensores: " + res.cause().getMessage());
//		     }
//				});
//	}
//	
	
	
	//Actuators
	
	protected void getActuatorById(RoutingContext routingContext) {
		int actuatorId = Integer.parseInt(routingContext.request().getParam("actuator"));
		mySqlClient.preparedQuery("SELECT * FROM wait.actuators WHERE idActuator = ?;", Tuple.of(actuatorId),
				res -> {
					if (res.succeeded()) {
						// Get the result set
						 RowSet<Row> resultSet = res.result();
		                 System.out.println(resultSet.size());
		                 List<Actuator> result = new ArrayList<>();
		                 for (Row elem : resultSet) {
		                 	result.add(new Actuator(elem.getInteger("idActuator"), elem.getString("name"),
									elem.getInteger("idDevice"),
									elem.getBoolean("removed")));
		                 }
						routingContext.response()
		                .putHeader("content-type", "application/json; charset=utf-8")
		                .setStatusCode(200)
		                .end(gson.toJson(result));
					} else {
						 System.out.println("Error: " + res.cause().getLocalizedMessage());
		                 routingContext.response().setStatusCode(500).end("Error al obtener los actuadores: " + res.cause().getMessage());
					}
				});
	}

	protected void addActuator(RoutingContext routingContext) {
		final Actuator actuator = gson.fromJson(routingContext.getBodyAsString(), Actuator.class);
		mySqlClient.preparedQuery(
				"INSERT INTO wait.actuators (name, idDevice, removed) VALUES (?,?,?);",
				Tuple.of(actuator.getName(), actuator.getIdDevice(),
						actuator.isRemoved()),
				res -> {
					if (res.succeeded()) {
						long lastInsertId = res.result().property(MySQLClient.LAST_INSERTED_ID);
						actuator.setIdActuator((int) lastInsertId);
						routingContext.response().setStatusCode(201).putHeader("content-type",
                                "application/json; charset=utf-8").end("Actuador añadido correctamente");
					} else {
						System.out.println("Error: " + res.cause().getLocalizedMessage());
                        routingContext.response().setStatusCode(500).end("Error al añadir el sensor: " + res.cause().getMessage());
					}
				});
	}
	
	protected void putActuator(RoutingContext routingContext) {
		final Actuator actuator = gson.fromJson(routingContext.getBodyAsString(), Actuator.class);
		
		mySqlClient.preparedQuery(
				"UPDATE wait.actuators g SET name = COALESCE(?, g.name), idDevice = COALESCE(?, g.idDevice),  removed = COALESCE(?, g.removed) WHERE idActuator = ?;",
				Tuple.of(actuator.getName(), actuator.getIdDevice(), actuator.isRemoved(),
						actuator.getIdActuator()),
				res -> {
					if (res.succeeded()) {
						//if (res.result().rowCount() > 0) {
	                        routingContext.response().setStatusCode(200).putHeader("content-type", 
	                        		"application/json; charset=utf-8").end(gson.toJson(actuator));
	                    //} 
					} else {
						System.out.println("Error: " + res.cause().getLocalizedMessage());
		  	              routingContext.response().putHeader("content-type", "application/json; charset=utf-8").setStatusCode(404).end("Error al actualizar los sensores: " + res.cause().getMessage());
					}
				});
	}
	
	protected void deleteActuator(RoutingContext routingContext) {
		int actuatorId = Integer.parseInt(routingContext.request().getParam("actuatorid"));
		mySqlClient.preparedQuery("DELETE FROM wait.actuators WHERE idActuator = ?;", Tuple.of(actuatorId),
				res -> {
					if (res.succeeded()) {
						if (res.result().rowCount() > 0) {
		                    routingContext.response()
		                        .setStatusCode(200)
		                        .putHeader("content-type", "application/json; charset=utf-8")
		                        .end(gson.toJson(new JsonObject().put("message", "Actuador eliminado correctamente")));
		                } 
					} else {
						System.out.println("Error: " + res.cause().getLocalizedMessage());
			            routingContext.response()
			                    .setStatusCode(500)
			                    .end("Error al conectar con la base de datos: ");
					}
				});
	}
	



	//ActuadorValues
	protected void  getLastActuatorStatus(RoutingContext routingContext) {
		int actuatorId = Integer.parseInt(routingContext.request().getParam("actuatorid"));
		mySqlClient.preparedQuery(
				"SELECT * FROM wait.actuatorstates WHERE idActuator = ? ORDER BY `timestamp` DESC LIMIT 1;",
				Tuple.of(actuatorId), res -> {
					if (res.succeeded()) {
						// Get the result set
						RowSet<Row> resultSet = res.result();
                        System.out.println(resultSet.size());
                        List<ActuatorStatus> result = new ArrayList<>();
                        for (Row elem : resultSet) {
                        	result.add(new ActuatorStatus(elem.getInteger("idActuatorState"),
									elem.getFloat("status"), elem.getBoolean("statusBinary"),
									elem.getInteger("idActuator"),elem.getLong("timestamp"),
									elem.getBoolean("removed")));
                        }
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .setStatusCode(200)
                                .end(gson.toJson(result));
                    } else {
                        System.out.println("Error: " + res.cause().getLocalizedMessage());
                        routingContext.response().setStatusCode(500).end("Error al obtener los estados de los actuadores: " + res.cause().getMessage());
                    }
				});
	}
	
	protected void addActuatorStatus(RoutingContext routingContext) {
		final ActuatorStatus actuatorState = gson.fromJson(routingContext.getBodyAsString(), ActuatorStatus.class);
		mySqlClient.preparedQuery(
				"INSERT INTO wait.actuatorstates (status, statusBinary, idActuator, timestamp, removed) VALUES (?,?,?,?,?);",
				Tuple.of(actuatorState.getStatus(), actuatorState.isStatusBinary(), actuatorState.getIdActuator(),
						actuatorState.getTimestamp(), actuatorState.isRemoved()),
				res -> {
					if (res.succeeded()) {
						long lastInsertId = res.result().property(MySQLClient.LAST_INSERTED_ID);
						actuatorState.setIdActuatorState((int) lastInsertId);
						routingContext.response().setStatusCode(201).putHeader("content-type",
                                "application/json; charset=utf-8").end("Estado Actuador añadido correctamente");
					} else {
						System.out.println("Error: " + res.cause().getLocalizedMessage());
                        routingContext.response().setStatusCode(500).end("Error al añadir el estado del actuador: " + res.cause().getMessage());
					}
				});
	}
	
	protected void deleteActuatorStatus(RoutingContext routingContext) {
		int idActuatorStatus = Integer.parseInt(routingContext.request().getParam("actuatorstatusid"));
		mySqlClient.preparedQuery("DELETE FROM wait.actuatorstates WHERE idActuatorState = ?;",
				Tuple.of(idActuatorStatus), res -> {
					if (res.succeeded()) {
						if (res.result().rowCount() > 0) {
		                    routingContext.response()
		                        .setStatusCode(200)
		                        .putHeader("content-type", "application/json; charset=utf-8")
		                        .end(gson.toJson(new JsonObject().put("message", "Estado del actuador eliminado correctamente")));
		                } 
					} else {
						System.out.println("Error: " + res.cause().getLocalizedMessage());
			            routingContext.response()
			                    .setStatusCode(500)
			                    .end("Error al conectar con la base de datos: ");
					}
				});
	}
	


	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		super.stop(stopFuture);
	}

}
